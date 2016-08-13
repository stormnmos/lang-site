(ns lang-site.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [datascript.core :as d]
            [lang-site.actions :as a]
            [lang-site.db :as db]
            [lang-site.util :as u]
            [sablono.core :as sab :include-macros true]
            [cljs.core.async :as async :refer [<! >! put! take!]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defmulti rend
  (fn [type [_ _] _]
    type))

(defmulti widgets
  (fn [[eid db] _]
    (db/g db :widget/type eid)))

(defmethod rend :header [_ [_ _] title links]
  (sab/html
   [:header.mdl-layout__header
    [:.mdl-layout__header-row
     [:span.mdl-layout-title title]
     [:.mdl-layout-spacer]
     [:nav.mdl-navigation.mdl-layout--larget-screen-only
      (map (fn [link]
             [:a.mdl-navigation__link {:href "#"} (:link/text link)])
           links)]]]))

(defmethod widgets :header [[eid db] owner]
  (reify
    om/IRender
    (render [this]
      (let [links
            (:widget/content (d/pull db [{:widget/content [:link/text]}]
                                     eid))]
        (rend :header [eid db] "Header" links)))))

(defmethod rend :header-drawer
  [_ [_ _] title links]
  (sab/html
   [:.mdl-layout__drawer
    [:span.mdl-layout-title title]
    [:nav.mdl-navigation
     (map (fn [link]
             [:a.mdl-navigation__link {:href "#"} (:link/text link)])
           links)]]))

(defmethod widgets :header-drawer [[eid db] owner]
  (reify
    om/IRender
    (render [this]
      (let [links (db/gets db {:widget/content [:link/text]} eid)]
        (rend :header-drawer [eid db] "Header-drawer" links)))))

(defmethod rend :page
  [_ [_ _] [header header-drawer]]
  (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
         (map u/make [header header-drawer])]))

(defmethod widgets :page [[eid db] owner]
  (reify
    om/IRender
    (render [this]
      (let [[[header] [header-drawer]]
            (db/get-widgets db [:header :header-drawer])]
        (rend :page [eid db] [header header-drawer])))))

(defmethod rend :card
  [_ [eid db] owner title texts]
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
    [:.mdl-card__actions.mdl-card--border
     [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
      {:on-click #(a/next-card eid db
                              (:events (om/get-shared owner)))}
      "Next Sentence"]]
    [:.mdl-card__menu
     [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
      [:i.material-icons "person"]]]]))

(defmethod widgets :card [[eid db] owner]
  (reify
    om/IRender
    (render [this]
      (let [title (db/g db :card/title eid)
            texts (db/gets db {:card/sentences [:sentence/text]} eid)]
        (rend :card [eid db] owner title texts)))))

(defmethod rend :grid
  [_ [_ _] components]
  (sab/html
   [:.mdl-grid
    (map (fn [component]
           [:.mdl-cell.mdl-cell--3-col (u/make widgets component)])
         (sort-by first components))]))

(defmethod widgets :grid [[eid db] owner]
  (reify
    om/IRender
    (render [this]
      (let [components (db/get-ui-comps db :app/grid-components)]
        (rend :grid [eid db] components)))))

(defn widget [conn owner]
  (reify
    om/IRender
    (render [this]
      (let [db @conn
            header        (db/get-widget db :header)
            header-drawer (db/get-widget db :header-drawer)
            grid          (db/get-widget db :grid)]
        (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
                   (u/make-all widgets [header header-drawer grid])])))))
