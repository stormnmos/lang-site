(ns lang-site.app
  (:require
   [goog.dom :as gdom]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]
   [sablono.core :as sab :refer-macros [html]]
   [datascript.core :as d]))

(enable-console-print!)

(def conn (d/create-conn
           {:translation/group {:db/cardinality :db.cardinality/many
                                :db/valueType :db.type/ref}}))

(d/transact! conn
   [{:db/id 0
     :app/title "Hello, DataScript!"
     :app/count 0}
    {:db/id -1
     :sentence/text "First Sentence"
     :sentence/group 100}
    {:db/id -2
     :sentence/text "Second Sentence"
     :sentence/group 100}
    {:db/id -3
     :translation/group #{-1 -2}}])

(defmulti read om/dispatch)

(defmethod read :default
  [_ _ _]
  nil)

(defmethod read :app/eid
  [{:keys [state query]} _ params]
  {:value (d/pull (d/db state) query (:eid params))})

(defmethod read :app/sentence
  [{:keys [state query]} _ params]
  {:value (d/q '[:find [(pull ?e ?selector) ...]
                 :in $ ?selector
                 :where [?e :sentence/text]]
               (d/db state) query)})

(defmethod read :translation/group
  [{:keys [state query]} _ _]
  {:value (d/q '[:find [(pull ?e ?selector) ...]
                 :in $ ?selector
                 :where [?e :translation/group]]
               (d/db conn) [{:translation/group query}])})

(defmethod read :app/counter
  [{:keys [state query]} _ _]
  {:value (d/q '[:find [(pull ?e ?selector) ...]
                 :in $ ?selector
                 :where [?e :app/title]]
               (d/db state) query)})

(defmethod read :default
  [{:keys [state]} _ _]
  {:value 10})

(defmulti mutate om/dispatch)

(defmethod mutate 'app/increment
  [{:keys [state]} _ entity]
  {:value {:keys [:app/counter]}
   :action (fn [] (d/transact! state
                     [(update-in entity [:app/count] inc)]))})


(defui Sentence
  static om/Ident
  (ident [this {:keys [db/id]}] [:db/id id])
  static om/IQueryParams
  (params [this]
          {:eid 1})
  static om/IQuery
  (query [this]
         '[({:app/eid [:sentence/text :sentence/group]} {:eid ?eid})])
  Object
  (render [this]
          (let [{:keys [sentence/text sentence/group ]}
                (get-in (om/props this) [:app/eid])]
            (dom/div nil
                     (dom/h2 nil "Text: " text)
                     (dom/h3 nil "Group: " group)))))

(def sentence (om/factory Sentence))

(defui ListView
  Object
  (render [this]
          (let [list (om/props this)]
            (dom/ul nil (mapv sentence list)))))

(def list-view (om/factory ListView))

(defui Translation
  static om/Ident
  (ident [this {:keys [db/id]}] [:db/id id])
  static om/IQuery
  (query [this]
         '[{:translation/group [:sentence/text]}])
  Object
  (render [this]
          (let [{:keys [:translation/group]} (om/props this)]
              (html
               [:div (list-view group)]))))

(defn to-int [str]
  (if (js/isNaN (js/parseInt str))
    0
    (js/parseInt str)))

(defui Counter
  static om/IQuery
  (query [this]
         [{:app/counter [:db/id :app/title :app/count]}])
  Object
  (render [this]
    (let [{:keys [app/title app/count] :as entity}
          (get-in (om/props this) [:app/counter 0])]
      (dom/div nil
        (dom/h2 nil title)
        (dom/span nil (str "Count: " count))
        (dom/button
         #js {:onClick
              (fn [e]
                (om/transact! this
                              `[(app/increment ~entity)]))}
         "Click me!")))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
   {:state conn
    :parser parser}))

(defn run []
  (om/add-root! reconciler Sentence (gdom/getElement "app")))
