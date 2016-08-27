(ns lang-site.components.snippets
  (:require [kioo.om :as k]
            [lang-site.actions :as a]
            [lang-site.util :as u])
  (:require-macros [kioo.om :refer [defsnippet deftemplate]]))

(deftemplate card "card.html"
  [{:keys [:db/id card/title card/content]}]
  {[:.translation-input] (k/set-attr :id  (str "translation" id))
   [:.translation-label] (k/set-attr :for (str "translation" id))
   [:.mdl-button]        (k/set-attr :on-click #(a/next-card id))})

(deftemplate default "default.html"
  [_]
  {[:div] (k/content "Default Component")})

(deftemplate grid "grid.html"
  [{:keys [:grid/content]}]
  {[:.mdl-grid] (k/content (u/make lang-site.components/widgets (:db/id content)))})

(deftemplate header "header.html"
  [{:keys [:header/title :header/content]}]
  {[:.mdl-layout-title] (k/content title)})

(deftemplate header-drawer "header-drawer.html"
  [{:keys [:header-drawer-title :header-drawer/content]}]
  {[:nav] (k/content (u/make-all lang-site.components/widgets (map :db/id  content)))})

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
  {[:.mdl-layout] (k/content (u/make-all lang-site.components/widgets (map :db/id content)))})

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
