(ns lang-site.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [datascript.core :as d]
            [lang-site.actions :as a]
            [lang-site.db :as db]
            [lang-site.util :as u]
            [lang-site.requests :as req]
            [lang-site.components.templates :as t]
            [sablono.core :as sab :include-macros true]
            [cljs.core.async :as async :refer [<! >! put! take!]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defprotocol Widget
  (children-tree [this])
  (remote [this])
  (local-call [this])
  (template [this data])
  (query [this]))

(defmulti widgets
  (fn [[eid db] _]
    (db/g db :widget/type eid)))

(defmethod widgets :header [[eid db] owner]
  (reify
    Widget
    (query [this]
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
      (let [[des pq] (query this)
            content (des (d/pull db pq eid))]
        (template this ["Language Site" content])))))

(defmethod widgets :header-drawer [[eid db] owner]
  (reify
    Widget
    (query [this]
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
      (let [[des pq] (query this)
            content (des (d/pull db pq eid))]
        (template this ["Header-drawer" content])))))

(defmethod widgets :page [[eid db] owner]
  (reify
    Widget
    (template [this [header header-drawer]]
      (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
                 (map u/make [header header-drawer])]))
    om/IRender
    (render [this]
      (let [[[header] [header-drawer]]
            (db/get-widgets db [:header :header-drawer])]
        (template this [header header-drawer])))))

(defmethod widgets :register-card [[eid db] owner]
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

(defmethod widgets :card [[eid db] owner]
  (reify
    Widget
    (template [this [owner title texts]]
      (sab/html
       [:.mdl-card.mdl-shadow--4dp.language-card
        [:.mdl-card__title-text
         [:h2.mdl-card__title-text (str "Card " eid)]]
        [:.mdl-card__supporting-text
         [:ul.mdl-list
          (map (fn [text] [:li.mdl-list__item
                           [:span.mdl-list__item-primary-content
                            (str (:sentence/text text) "\n")]])
               texts)]]
        [:form {:action "#"}
         [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
          [:input.mdl-textfield__input {:type "text" :id (str "translation" eid)}]
          [:label.mdl-textfield__label {:for (str "translation" eid)}
           "Translation"]]]
        [:.mdl-card__actions.mdl-card--border
         [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
          {:on-click #(a/next-card eid db
                                   (:events (om/get-shared owner)))
           :disabled false}
          "Next Sentence"]]
        [:.mdl-card__menu
         [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
          [:i.material-icons "person"]]]]))
    (remote [this]
      "/translation-group")
    om/IRender
    (render [this]
      (let [title (db/g db :card/title eid)
            texts (db/gets db {:card/sentences [:sentence/text]} eid)]
        (template this [owner title texts])))
    om/IDidMount
    (did-mount [this]
      (if (> 10 (count (d/datoms db :avet :widget/type :card)))
        (mapv #(req/http-get (remote this) t/card)
              (range 30))))))

(defmethod widgets :grid [[eid db] owner]
  (reify
    Widget
    (template [this [components]]
      (sab/html
       [:.mdl-grid
        (map (fn [component]
               [:.mdl-cell.mdl-cell--3-col (u/make widgets component)])
             (sort-by first components))]))
    om/IRender
    (render [this]
      (let [components (db/get-ui-comps db :app/grid-components)]
        (template this [components])))))

(defn widget [conn owner]
  (reify
    om/IRender
    (render [this]
      (let [db @conn
            header        (db/get-widget db :header)
            header-drawer (db/get-widget db :header-drawer)
            grid          (db/get-widget db :grid)
            register      (db/get-widget db :register-card)]
        (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
                   (u/make-all widgets [header header-drawer grid register])])))))
