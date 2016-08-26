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
  (template
   [this data]
   [:.default "Default component"]))

(defwidget :menu-item
  (template
   [this {:keys [:menu-item/text]}]
   [:li.mdl-menu__item text]))

(defwidget :link
  (template
   [this {:keys [:link/text :link/icon :link/href]}]
   [:a.mdl-navigation__link {:href href}
    [:i.mdl-color-text--blue-grey-400.material-icons {:role "presentation"} icon]
    text]))

(defwidget :header
  (template
   [this {:keys [:header/title :header/content]}]
   [:header.app-header.mdl-layout__header.mdl-color--grey-100.mdl-color-text--grey-600
    [:.mdl-layout__header-row
     [:span.mdl-layout-title title]
     [:.mdl-layout-spacer]
     [:mdl-textfield.mdl-js-textfield.mdl-textfield--expandable
      [:label.mdl-button.mdl-js-button.mdl-button--icon {:for "search"}
       [:i.material-icons "search"]]
      [:mdl-textfield__expandable-holder
       [:input.mdl-textfield__input {:type "text" :id "search"}]
       [:label.mdl-textfield__label {:for "search"} "Enter your query"]]]
     [:button.mdl-button.mdl-js-button.mdl-js-ripple-effect.mdl-button--icon {:id "hdrbtn"}
      [:i.material-icons "more_vert"]]
     [:ul.mdl-menu.mdl-js-menu.mdl-js-ripple-effect.mdl-menu--bottom-right {:for "hdrbtn"}
      [:li.mdl-menu__item "About"]
      [:li.mdl-list__item "Contact"]
      [:li.mdl-list__item "Legal Information"]]]]))

(defwidget :header-drawer
  (template
   [this {:keys [:header-drawer/title :header-drawer/content]}]
   [:.app-drawer.mdl-layout__drawer.mdl-color--blue-grey-900.mdl-color-text--blue-grey-50
    [:header.app-drawer-header
     [:demo-avatar-dropdown
      [:span "hello@example.com"]
      [:.mdl-layout-spacer]
      [:button.mdl-button.mdl-js-button.mdl-js-ripple-effect.mdl-button--icon
       [:i.material-icons {:role "presentation"} "arrow_drop_down"]
       [:span.visuallyhidden "Accounts"]]
      [:ul.mdl-menu.mdl-menu--bottom-right.mdl-js-menu.mdl-js-ripple-effect {:for "accbtn"}
       [:li.mdl-list__item "hello@example.com"]]]]
    [:nav.app-navigation.mdl-navigation.mdl-color--blue--grey-800
     (u/make-all widgets (map :db/id content))]]))

(defwidget :register-card
  (template
   [this {:keys [:db/id]}]
   [:.mdl-card.mdl-shadow--4dp.register-user-card
    [:.mdl-card__title-text
     [:.h2.mdl-card__title-text (str "Register")]]
    [:.mdl-card__supporting-text.mdl-card--border
     [:form {:action "#"}
      [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
       [:input.mdl-textfield__input {:type "text" :id (str "register" id)}]
       [:label.mdl-textfield__label {:for (str "register" id)} "Username"]]
      [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
       [:input.mdl-textfield__input {:type "password" :id (str "password" id)}]
       [:label.mdl-textfield__label {:for (str "password" id)} "Password"]]]]
    [:.mdl-card__actions.mdl-card--border
     [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
      {:on-click #(a/next-card id)} "Submit"]]]))

(defwidget :login-card
  (template
   [this {:keys [:db/id]}]
   [:.mdl-card.mdl-shadow--4dp.register-user-card
    [:.mdl-card__title-text
     [:.h2.mdl-card__title-text (str "Login")]]
    [:.mdl-card__supporting-text.mdl-card--border
     [:form {:action "#"}
      [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
       [:input.mdl-textfield__input {:type "text" :id (str "register" id)}]
       [:label.mdl-textfield__label {:for (str "register" id)} "Username"]]
      [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
       [:input.mdl-textfield__input {:type "password" :id (str "password" id)}]
       [:label.mdl-textfield__label {:for (str "password" id)} "Password"]]]]
    [:.mdl-card__actions.mdl-card--border
     [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
      {:on-click #(a/next-card id)} "Login"]]]))

(defwidget :user
  (template
   [this {:keys [:user/name :user/email]}]
   [:.mdl-card.mdl-shadow--4dp.user-card
    [:.mdl-card__title-text
     [:.h4.mdl-card__title-text "User"]]
    [:.mdl-card__supporting-text.mdl-card--border
     [:h2 name]
     [:h3 email]]]))

(defwidget :user-card
  (template
   [this {{:keys [:user/name :user/email]} :user-card/user}]
   [:.mdl-card.mdl-shadow--4dp.user-card
    [:.mdl-card__title-text
     [:.h4.mdl-card__title-text "User"]]
    [:.mdl-card__supporting-text.mdl-card--border
     [:h2 name]
     [:h3 email]]]))

(defwidget :sentence
  (template
   [this {:keys [:sentence/text]}]
   [:li.mdl-list__item
    [:span.mdl-list__item-primary-content text]]))

(defwidget :card
  (template
   [this {:keys [:db/id card/title card/content]}]
   [:.mdl-card.mdl-shadow--4dp.language-card
    [:.mdl-card__title-text
     [:h2.mdl-card__title-text (str "Card")]]
    [:.mdl-card__supporting-text
     [:ul.mdl-list
      (u/make-all widgets (map :db/id content))]]
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
      [:i.material-icons "person"]]]])
  (remote
   [this]
   "/translation-group")
  om/IDidMount
  (did-mount
   [this]
   (if (> 10 (count (d/datoms (d/db @conn) :avet :widget/type :card)))
     (mapv #(req/http-get (remote this) t/card) (range 10)))))

(defwidget :grid
  (template
   [this {:keys [:grid/content]}]
   [:.mdl-grid
    (map (fn [entity]
           [:.mdl-cell.mdl-cell--6-col (u/make widgets (:db/id entity))])
         (sort-by first content))]))

(defwidget :page
  (template
   [this {:keys [:page/content]}]
   [:.mdl-layout.mdl-js-layout.mdl-layout--fixed-header.mdl-layout--fixed-drawer
    (u/make-all widgets (map :db/id content))]))

(defn widget [_]
  (reify
    om/IRender
    (render [this]
      (let [page (db/get-widget :page)]
        (sab/html [:.app (u/make-all widgets [page])])))))
