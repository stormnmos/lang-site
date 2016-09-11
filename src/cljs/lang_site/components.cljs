(ns lang-site.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [datascript.core :as d]
            [datascript.transit :as dt]
            [kioo.om :as k]
            [lang-site.actions :as a]
            [lang-site.db :as db]
            [lang-site.util :as u]
            [lang-site.requests :as req]
            [lang-site.state :refer [conn events]]
            [lang-site.components.snippets :as s]
            [lang-site.components.templates :as t]
            [cljs.core.async :as async :refer [<! >! chan put! take! tap offer!]]
            [clojure.string :as sring])
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
  [{:keys [:db/id :card/title]
    {answer-sentence :sentence/text
     :or {answer-sentence "answer not found"}} :card/answer
    {question-sentence :sentence/text
     :or {question-sentence "question not found"}} :card/question}]
  {[:.translation-input] (k/set-attr :id  (str "translation" id))
   [:.translation-label] (k/set-attr :for (str "translation" id))
   [:.card-question]     (k/content question-sentence)
   [:.card-answer]       (k/content answer-sentence)
   [:.mdl-button]        (k/listen :on-click #(a/next-card id))
   [:.mdl-cell]          (k/listen :onKeyPress #(.log js/console %))
   [:.b1]                (k/content "Test1")
   [:.b2]                (k/content "Test2")
   [:.b3]                (k/content "Test3")
   [:.b4]                (k/content "Test4")})

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
  [{{:keys [:db/id :temp/user :temp/email :temp/password]} :register-card/temp}]
  {[:.username-input] (k/listen :onKeyUp (partial a/track-input id :temp/user))
   [:.email-input]    (k/listen :onKeyUp (partial a/track-input id :temp/email))
   [:.password-input] (k/listen :onKeyUp (partial a/track-input id :temp/password))
   [:.mdl-button]
   (k/set-attr
    :onClick #(req/http-post "/api/users"
                {:user user :email email :password password}))})

(deftemplate sentence "sentence.html"
  [{:keys [:sentence/text]}]
  {[:span] (k/content text)})

(deftemplate user-card "user-card.html"
  [{{:keys [:user/name :user/email :user/password]} :user-card/user
    id :db/id :as user-card
    data :user-card/data}]
  {[:.name]       (k/content name)
   [:.email]      (k/content email)
   [:.password]   (k/content password)
   [:.mdl-button] (k/listen :on-click #(a/remove-eid id))
   [:.users-data] (k/content data)})

(defwidget :default default)
(defwidget :widget/menu-item menu-item)
(defwidget :widget/link link)
(defwidget :widget/header header)
(defwidget :widget/header-drawer header-drawer)
(defwidget :widget/register-card register-card)
(defwidget :widget/login-card login-card)
(defwidget :widget/user-card user-card)
(defwidget :widget/sentence sentence)
(defwidget :widget/card card
  (remote
   [this]
   "/translation-group")
  om/IDidMount
  (did-mount
   [this]
   (if (> 10 (count (d/datoms (d/db @conn) :avet :widget/type :widget/card)))
     (mapv #(req/http-get (remote this) t/card) (range 10)))))
(defwidget :widget/grid grid)
(defwidget :widget/page page
  #_ om/IDidUpdate
  #_ (did-update [_ _ _]
       (u/persist (d/db @conn)))
  om/IDidMount
  (did-mount
   [this]
   nil
   #_(req/http-get "/api/users" t/make-users)))

(defn widget [_]
  (reify
    om/IRender
    (render [this]
      (u/make widgets (db/get-widget :widget/page)))))
