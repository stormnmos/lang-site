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
            [lang-site.spec :as spec]
            [lang-site.components.templates :as t]
            [cljs.core.async :as async :refer [<! >! chan put! take! tap offer!]]
            [cljs.spec :as s :include-macros true]
            [clojure.string :as sring])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [kioo.om :refer [deftemplate]]
   [lang-site.components :refer [defwidget]]))

(defn eid->entity [eid]
  {:pre  [(s/valid? :widget/ref eid)]
   :post [(s/valid? map? %)]}
  (->> eid
       (d/entity (d/db @conn))
       (d/touch)))

(defn make [f eid]
  (s/assert :widget/widget (eid->entity eid))
  (om/build f eid {:react-key eid}))

(defn make-all [f eids]
  {:pre [(s/valid? :widget/content eids)]}
  (map (partial make f) eids))

(defprotocol Widget
  (remote     [this]))

(defmulti widgets
  (fn [eid _]
    (db/g :widget/type eid)))

(deftemplate card "card.html"
  [{:keys [:db/id :card/title]
    {answer-sentence :sentence/text
     :or {answer-sentence "answer not found"}} :card/answer
    {question-sentence :sentence/text
     :or {question-sentence "question not found"}} :card/question} owner]
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

(deftemplate container "container.html"
  [{:keys [:container/content]} owner]
  {[:.row] (k/content (make-all widgets (map :db/id content)))})

(deftemplate default "default.html"
  [_ _]
  {[:div] (k/content "Default Component")})

(deftemplate footer "footer.html"
  [{:keys [:footer/left-links :footer/right-links]} owner]
  {[:.left-list] (k/content (make-all widgets (map :db/id left-links)))
   [:.right-list] (k/content (make-all widgets (map :db/id right-links)))})

(deftemplate grid "grid.html"
  [{:keys [:grid/data :grid/content]} owner]
  {#_#_[:.mdl-debug] (k/content (str content))
   [:.mdl-grid] (k/content (make-all widgets (map :db/id content)))})

(deftemplate header "header.html"
  [{:keys [:header/title :header/content]} owner]
  {[:.mdl-layout-title] (k/content title)})

(deftemplate header-drawer "header-drawer.html"
  [{:keys [:header-drawer/title :header-drawer/content]} owner]
  {[:nav] (k/content (make-all widgets (map :db/id content)))})

(deftemplate link "link.html"
  [{:keys [:link/text :link/icon :link/href]} owner]
  {[:a] (k/set-attr :href href)
   [:i] (k/content icon)})

(deftemplate login-card "login-card.html"
  [{:keys [:db/id]} owner]
  {[:.username-input] (k/set-attr :id  (str "register" id))
   [:.username-label] (k/set-attr :for (str "register" id))
   [:.password-input] (k/set-attr :id  (str "password" id))
   [:.password-label] (k/set-attr :for (str "password" id))
   [:.mdl-button]     (k/set-attr :on-click #(a/next-card id))})

(deftemplate menu-item "menu-item.html"
  [{:keys [:menu-item/text]} owner]
  {[:li] (k/content text)})

(deftemplate nav "nav.html"
  [{:keys [:db/id :nav/title :nav/links]} owner]
  {[:.title] (k/content title)
   [:.nav-links] (k/content (make-all widgets (map :db/id links)))})

(deftemplate nav-link "nav-link.html"
  [{:keys [:db/id :nav-link/text :nav-link/href]} owner]
  {[:a] (k/do->
         (k/content text)
         (k/set-attr :href href))})

(deftemplate page "page.html"
  [{:keys [page/content]} owner]
  {[:.app] (k/content (make-all widgets (map :db/id content)))})

(deftemplate register-card "register-card.html"
  [{:keys [:db/id
           :register-card/user
           :register-card/email
           :register-card/password]} owner]
  {[:.username-input]
   (k/set-attr :ref "user")
   [:.email-input]
   (k/set-attr :ref "email")
   [:.password-input]
   (k/set-attr :ref "password")
   [:.mdl-button]
   (k/set-attr
    :onClick #(req/http-post
               "/api/users"
               {:user (.-value (om/get-node owner "user"))
                :email (.-value (om/get-node owner "email"))
                :password (.-value (om/get-node owner "password"))}))})

(deftemplate sentence "sentence.html"
  [{:keys [:sentence/text]} owner]
  {[:span] (k/content text)})

(deftemplate sidebar "sidebar.html"
  [{:keys [:sidebar/links1 :sidebar/links2 :sidebar/links3]} owner]
  {[:.sidebar1] (k/content (make-all widgets (map :db/id links1)))
   [:.sidebar2] (k/content (make-all widgets (map :db/id links2)))
   [:.sidebar3] (k/content (make-all widgets (map :db/id links3)))})

(deftemplate sidebar-link "sidebar-link.html"
  [{:keys [:sidebar-link/text :sidebar-link/href]} owner]
  {[:a] (k/do-> (k/set-attr :href href)
                (k/content text))})

(deftemplate user-card "user-card.html"
  [{{:keys [:user/name :user/email :user/password]} :user-card/user
    id :db/id :as user-card
    data :user-card/data} owner]
  {[:.name]       (k/content name)
   [:.email]      (k/content email)
   [:.password]   (k/content password)
   [:.mdl-button] (k/listen :on-click #(a/remove-eid id))
   [:.users-data] (k/content data)})

(defwidget :default default)
(defwidget :widget/container container)
(defwidget :widget/footer footer)
(defwidget :widget/menu-item menu-item)
(defwidget :widget/link link)
(defwidget :widget/header header)
(defwidget :widget/header-drawer header-drawer)
(defwidget :widget/register-card register-card)
(defwidget :widget/nav nav)
(defwidget :widget/nav-link nav-link)
(defwidget :widget/login-card login-card)
(defwidget :widget/user-card user-card)
(defwidget :widget/sentence sentence)
(defwidget :widget/sidebar sidebar)
(defwidget :widget/sidebar-link sidebar-link)
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
      (make widgets (db/get-widget :widget/page)))))
