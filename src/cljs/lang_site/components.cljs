(ns lang-site.components
  (:require [om.core :as om :include-macros true]
            [datascript.core :as d]
            [datascript.transit :as dt]
            [kioo.om :as k]
            [lang-site.actions :as a]
            [lang-site.db :as db]
            [lang-site.util :as u]
            [lang-site.requests :as req]
            [lang-site.state :refer [conn events transactions]]
            [lang-site.components.snippets :as s]
            [lang-site.components.templates :as t]
            [sablono.core :as sab :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan put! take! tap offer!]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [kioo.om :refer [deftemplate]]
   [lang-site.components :refer [defwidget]]))

(defprotocol Widget
  (children   [this])
  (remote     [this]))

(defmulti widgets
  (fn [eid _]
    (db/g :widget/type eid)))

(deftemplate card "card.html"
  [{:keys [:db/id card/title card/content]}]
  {[:.translation-input] (k/set-attr :id  (str "translation" id))
   [:.translation-label] (k/set-attr :for (str "translation" id))
   [:.mdl-list]          (k/content (u/make-all widgets (map :db/id content)))
   [:.mdl-button]        (k/listen :on-click #(a/next-card id))
   [:.mdl-cell]          (k/listen :onKeyPress #(.log js/console %))})

(deftemplate default "default.html"
  [_]
  {[:div] (k/content "Default Component")})

(deftemplate grid "grid.html"
  [{:keys [:grid/data :grid/content]}]
  {#_#_[:.mdl-debug] (k/content (str content))
   [:.mdl-grid] (k/content (u/make-all widgets (map :db/id content)))})

(deftemplate header "header.html"
  [{:keys [:header/title :header/content]}]
  {[:.mdl-layout-title] (k/content title)})

(deftemplate header-drawer "header-drawer.html"
  [{:keys [:header-drawer/title :header-drawer/content]}]
  {[:nav] (k/content (u/make-all widgets (map :db/id content)))})

(deftemplate link "link.html"
  [{:keys [:link/text :link/icon :link/href]}]
  {[:a] (k/set-attr :href href)
   [:i] (k/content icon)})

(deftemplate login-card "login-card.html"
  [{:keys [:db/id]}]
  {[:.username-input] (k/set-attr :id  (str "register" id))
   [:.username-label] (k/set-attr :for (str "register" id))
   [:.password-input] (k/set-attr :id  (str "password" id))
   [:.password-label] (k/set-attr :for (str "password" id))
   [:.mdl-button]     (k/set-attr :on-click #(a/next-card id))})

(deftemplate menu-item "menu-item.html"
  [{:keys [:menu-item/text]}]
  {[:li] (k/content text)})

(deftemplate page "page.html"
  [{:keys [page/content]}]
  {[:.mdl-layout] (k/content (u/make-all widgets (map :db/id content)))})

(deftemplate register-card "register-card.html"
  [{:keys [:db/id]}]
  {[:.username-input] (k/set-attr :id  (str "register" id))
   [:.username-label] (k/set-attr :for (str "register" id))
   [:.password-input] (k/set-attr :id  (str "password" id))
   [:.password-label] (k/set-attr :for (str "password" id))
   [:.mdl-button]     (k/set-attr :on-click #(a/next-card id))})

(deftemplate sentence "sentence.html"
  [{:keys [:sentence/text]}]
  {[:span] (k/content text)})

(deftemplate user-card "user-card.html"
  [{:keys [:user-card/user :user-card/email]}]
  {[:div] (k/content user)})

(defwidget :default default)
(defwidget :menu-item menu-item)
(defwidget :link link)
(defwidget :header header)
(defwidget :header-drawer header-drawer)
(defwidget :register-card register-card)
(defwidget :login-card login-card)
(defwidget :user-card user-card)
(defwidget :sentence sentence)
(defwidget :card card
  (remote
   [this]
   "/translation-group")
  om/IDidMount
  (did-mount
   [this]
   (if (> 10 (count (d/datoms (d/db @conn) :avet :widget/type :card)))
     (mapv #(req/http-get (remote this) t/card) (range 10)))))
(defwidget :grid grid)
(defwidget :page page
  om/IDidUpdate
  (did-update [_ _ _]
     (u/persist (d/db @conn))))

(defn widget [_]
  (reify
    om/IRender
    (render [this]
      (let [header (db/get-widget :header)
            page   (db/get-widget :page)]
        (sab/html [:.app (u/make-all widgets [page])])))))
