(ns lang-site.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [datascript.core :as d]
            [lang-site.actions :as a]
            [lang-site.db :as db]
            [lang-site.util :as u]
            [lang-site.requests :as req]
            [lang-site.state :refer [conn events transactions]]
            [lang-site.components.templates :as t]
            [sablono.core :as sab :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan put! take! tap]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defprotocol Widget
  (children   [this])
  (remote     [this])
  (local-call [this])
  (template   [this data])
  (query      [this db]))

(defmulti widgets
  (fn [eid _]
    (db/g :widget/type eid)))

(defmethod widgets :default [eid]
  (reify
    om/IRender
    (render [this]
      (sab/html
       [:.default (str "Default Component, eid: " eid)]))))

(defmethod widgets :header [eid]
  (reify
    Widget
    (children [this]
      nil)
    (query [this db]
      [:widget/content [{:widget/content [:link/text]}]])
    (template [this [title links]]
      (sab/html
       [:header.mdl-layout__header
        [:.mdl-layout__header-row
         [:span.mdl-layout-title title]
         [:.mdl-layout-spacer]
         [:nav.mdl-navigation.mdl-layout--larget-screen-only
          (map (fn [link]
                 [:a.mdl-navigation__link {:href "#"} (:link/text link)])
               links)]]]))
    om/IRender
    (render [this]
      (let [[destructure pull-query] (query this (d/db @conn))
            content (destructure (d/pull (d/db @conn) pull-query eid))]
        (template this ["Language Site" content])))))

(defmethod widgets :header-drawer [eid]
  (reify
    Widget
    (children [this]
      nil)
    (query [this db]
      [:widget/content [{:widget/content [:link/text]}]])
    (template [this [title links]]
      (sab/html
       [:.mdl-layout__drawer
        [:span.mdl-layout-title title]
        [:nav.mdl-navigation
         (map (fn [link]
                [:a.mdl-navigation__link {:href "#"} (:link/text link)])
              links)]]))
    om/IRender
    (render [this]
      (let [[des pq] (query this (d/db @conn))
            content (des (d/pull (d/db @conn) pq eid))]
        (template this ["Header-drawer" content])))))

(defmethod widgets :page [eid]
  (reify
    Widget
    (children [this]
      (db/get-widgets [:header :header-drawer]))
    (template [this [header header-drawer]]
      (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
                 (map u/make [header header-drawer])]))
    om/IRender
    (render [this]
      (template this (children this)))))

(defmethod widgets :register-card [eid]
  (reify
    Widget
    (template [this _]
      (sab/html
       [:.mdl-card.mdl-shadow--4dp.register-user-card
        [:.mdl-card__title-text
         [:.h2.mdl-card__title-text (str "Register")]]
        [:.mdl-card__supporting-text.mdl-card--border
         [:form {:action "#"}
          [:.mdl-textfield.mdl-js-textfield
           [:input.mdl-textfield__input {:type "text" :id (str "register" eid)}]
           [:label.mdl-textfield__label {:for (str "register" eid)}
            "Username"]]]]
        [:.mdl-card__actions.mdl-card--border]]))
    om/IRender
    (render [this]
      (template this nil))))

(defmethod widgets :sentence [eid]
  (reify
    Widget
    (template [this text]
      (sab/html
       [:li.mdl-list__item
        [:span.mdl-list__item-primary-content text]]))
    (query [this db]
      (d/touch (d/entity (d/db @conn) eid)))
    om/IRender
    (render [this]
      (let [{:keys [sentence/text]} (query this (d/db @conn))]
        (template this text)))))

(defmethod widgets :card [eid owner]
  (reify
    Widget
    (template [this [title sentences]]
      (sab/html
       [:.mdl-card.mdl-shadow--4dp.language-card
        [:.mdl-card__title-text
         [:h2.mdl-card__title-text (str "Card " eid)]]
        [:.mdl-card__supporting-text
         [:ul.mdl-list
          (u/make-all widgets (map :db/id  sentences))]]
        [:form {:action "#"}
         [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
          [:input.mdl-textfield__input {:type "text" :id (str "translation" eid)}]
          [:label.mdl-textfield__label {:for (str "translation" eid)}
           "Translation"]]]
        [:.mdl-card__actions.mdl-card--border
         [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
          {:on-click #(a/next-card eid)
           :disabled false}
          "Next Sentence"]]
        [:.mdl-card__menu
         [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
          [:i.material-icons "person"]]]]))
    (remote [this]
      "/translation-group")
    (query [this db]
      (d/touch (d/entity db eid)))
    om/IInitState
    (init-state [this]
      {:listener (async/chan (async/sliding-buffer 10))})
    om/IRender
    (render [this]
      (let [{:keys [card/title card/sentences]} (query this (d/db @conn))]
        (template this [title sentences])))
    om/IWillMount
    (will-mount [this]
      (let [listener (om/get-state owner :listener)]
        (d/listen! @conn eid #(put! listener %))))
    om/IDidMount
    (did-mount [this]
      (if (> 10 (count (d/datoms (d/db @conn) :avet :widget/type :card)))
        (mapv #(req/http-get (remote this) t/card)
              (range 10))))
    om/IWillUnmount
    (will-unmount [_]
      (.log js/console (str "Unmounting: " eid))
      (d/unlisten! @conn eid))
    om/IShouldUpdate
    (should-update [this _ _]
      (when-let [tx-report (async/poll! (om/get-state owner :listener))]
        (not (= (query this (:db-before tx-report))
                (query this (:db-after  tx-report))))))))

(defmethod widgets :grid [eid owner]
  (reify
    Widget
    (query [this db]
      (d/touch (d/entity db eid)))
    (template [this [components]]
      (sab/html
       [:.mdl-grid
        (map (fn [component]
               [:.mdl-cell.mdl-cell--3-col (u/make widgets (:db/id component))])
             (sort-by first components))]))
    om/IInitState
    (init-state [this]
      {:listener (async/chan (async/sliding-buffer 10))})
    om/IRender
    (render [this]
      (let [{:keys [:grid/components]} (query this (d/db @conn))]
        (template this [components])))
    om/IWillMount
    (will-mount [this]
      (let [listener (om/get-state owner :listener)]
        (d/listen! @conn eid #(put! listener %))))
    om/IWillUnmount
    (will-unmount [_]
      (.log js/console (str "Unmounting: " eid))
      (d/unlisten! @conn eid))
    om/IShouldUpdate
    (should-update [this _ _]
      true)))

(defn widget [_]
  (reify
    om/IRender
    (render [this]
      (let [db @conn
            header        (db/get-widget :header)
            header-drawer (db/get-widget :header-drawer)
            grid          (db/get-widget :grid)
            register      (db/get-widget :register-card)
            card          (db/get-widget :card)]
        (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
                   (u/make-all widgets [header
                                        header-drawer
                                        grid
                                        register])])))))
