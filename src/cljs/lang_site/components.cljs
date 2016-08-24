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
   [cljs.core.async.macros :refer [go go-loop]]
   [lang-site.components :refer [defwidget]]))

(defprotocol Widget
  (children   [this])
  (remote     [this])
  (local-call [this])
  (template   [this data])
  (query      [this db]))

(defmulti widgets
  (fn [eid _]
    (db/g :widget/type eid)))

(defwidget :default
  (template [this data]
    [:.default "Default component"]))

(defwidget :link
  (template [this {:keys [:link/text]}]
    [:a.mdl-navigation__link {:href "#"} (:link/text text)]))

(defwidget :header
  (template [this {:keys [:header/title :header/content]}]
    [:header.mdl-layout__header
     [:.mdl-layout__header-row
      [:span.mdl-layout-title title]
      [:.mdl-layout-spacer]
      [:nav.mdl-navigation.mdl-layout--larget-screen-only
       (u/make-all widgets (map :db/id content))]]]))

(defwidget :header-drawer
  (template [this {:keys [:header-drawer/title :header-drawer/content]}]
    [:.mdl-layout__drawer
     [:span.mdl-layout-title title]
     [:nav.mdl-navigation
      (u/make-all widgets (map :db/id content))]]))

(defwidget :register-card
  (template [this {:keys [:db/id]}]
    [:.mdl-card.mdl-shadow--4dp.register-user-card
     [:.mdl-card__title-text
      [:.h2.mdl-card__title-text (str "Register")]]
     [:.mdl-card__supporting-text.mdl-card--border
      [:form {:action "#"}
       [:.mdl-textfield.mdl-js-textfield
        [:input.mdl-textfield__input {:type "text" :id (str "register" id)}]
        [:label.mdl-textfield__label {:for (str "register" id)}
         "Username"]]]]
     [:.mdl-card__actions.mdl-card--border]]))

(defwidget :sentence
  (template [this {:keys [:sentence/text]}]
    [:li.mdl-list__item
     [:span.mdl-list__item-primary-content text]]))

(defwidget :card
  (template [this {:keys [:db/id card/title card/sentences]}]
    [:.mdl-card.mdl-shadow--4dp.language-card
     [:.mdl-card__title-text
      [:h2.mdl-card__title-text (str "Card")]]
     [:.mdl-card__supporting-text
      [:ul.mdl-list
       (u/make-all widgets (map :db/id sentences))]]
     [:form {:action "#"}
      [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
       [:input.mdl-textfield__input {:type "text" :id (str "translation" id)}]
       [:label.mdl-textfield__label {:for (str "translation" id)}
        "Translation"]]]
     [:.mdl-card__actions.mdl-card--border
      [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
       {:on-click #(a/next-card id)
        :disabled false}
       "Next Sentence"]]
     [:.mdl-card__menu
      [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
       [:i.material-icons "person"]]]]))

(defwidget :grid
  (template [this {:keys [:grid/components]}]
    [:.mdl-grid
     (map (fn [component]
            [:.mdl-cell.mdl-cell--3-col (u/make widgets (:db/id component))])
          (sort-by first components))]))

(defn widget [_]
  (reify
    om/IRender
    (render [this]
      (let [db @conn
            header        (db/get-widget :header)
            header-drawer (db/get-widget :header-drawer)
            grid          (db/get-widget :grid)
            register      (db/get-widget :register-card)]
        (sab/html [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
                   (u/make-all widgets [header header-drawer
                                        grid register])])))))
