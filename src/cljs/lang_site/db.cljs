(ns lang-site.db
  (:require [datascript.core :as d]
            [datascript.db :as ddb]
            [lang-site.db.mock-data :as m]))

(defn create-db []
  (d/create-conn m/schema))

(defn populate-db! [conn]
  (d/transact! conn m/fixtures))

(defn pea [db att]
  (d/q '[:find (pull ?e [*])
         :in $ ?a
         :where [?e ?a]] db att))

(defn pvea [db eid att]
  (d/q '[:find (pull ?v [*])
         :in $ ?e ?a
         :where [?e ?a ?v]] db eid att))

(defn vea [db eid att]
  (d/q '[:find ?v .
         :in $ ?e ?a
         :where [?e ?a ?v]]
       db eid att))

(defn eav [db att v]
  (d/q '[:find ?e
         :in $ ?a ?v
         :where [?e ?a ?v]]
       db att v))

(defn ea [db att]
  (d/q '[:find ?e
         :in $ ?a
         :where [?e ?a]]
       db att))

(defn g [db att eid]
  (att (d/pull db [att] eid)))

(defn gets [db att eid]
  ((first (keys att)) (d/pull db [att] eid)))

(defn gv [db atts eid]
  (map (fn [att] (g db att eid)) atts))

(defn children
  ([db eid]
   (map conj (eav db :widget/owner eid) (repeat db)))
  ([db vals eid]))

(defn ordered-children [db eid]
  (apply map vector
         [(->> (d/pull db [{:widget/_owner [:db/id :widget/order]}] eid)
                :widget/_owner
                (sort-by :widget/order)
                (map :db/id))
          (repeat db)]))

(defn get-widgets [db type]
  (map conj (d/q '[:find ?e
                   :in $ ?v
                   :where [?e :widget/type ?v]]
                 db type)
       (repeat db)))

(defn get-widget [db type]
  [(d/q '[:find ?e .
          :in $ ?v
          :where [?e :widget/type ?v]]
        db type)
   db])

(defn get-ui-att [db att]
  (g db att 0))

(defn get-ui-comps [db att]
  (mapv (fn [eid] [(:db/id eid) db])
        (att (d/pull db [{att [:db/id]}] 0))))

(defn set-att [eid att val]
  {:db/id eid
   att val})

(defn set-content [eid content]
  (set-att eid :widget/content content))

(defn get-att [db att]
  (d/q '[:find ?v .
         :in $ ?a
         :where [_ ?a ?v]]
       db att))

#_(defn cas [db e a ov nv]
  (let [e (ddb/entid-strict db e)
        _  (ddb/validate-attr db a)
        ov (if (ddb/ref? db a) (ddb/entid-strict db ov) ov)
        nv (if (ddb/ref? db a) (ddb/entid-strict db nv) nv)
        datoms ()]))
