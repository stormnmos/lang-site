(ns lang-site.components.templates
  (:require [cljs.spec :as s]))

(defn widget [id type]
  {:db/id id
   :widget/type type})

(defn make [type & keys]
  (fn [id & data]
    (merge (widget id type)
           (zipmap keys data))))

(def cloze-card-t (make :widget/cloze-card :cloze-card/title :cloze-card/question :cloze-card/answer))
(def sentence-t (make :widget/sentence :sentence/text :sentence/group :sentence/language))
(def link-t (make :widget/link :link/text :link/icon :link/href))
(def user-t (make :widget/user :user/name :user/email :user/password))
(def header-t (make :widget/header :header/title :header/content))
(def header-drawer-t (make :widget/header-drawer :header-drawer/title :header-drawer/content))
(def menu-item-t (make :widget/menu-item :menu-item/text))
(def grid-t (make :widget/grid :grid/data :grid/content))
(def page-t (make :widget/page :page/content))
(def card-t (make :widget/card :card/title :card/question :card/answer))
(def register-card-t (make :widget/register-card :register-card/user :register-card/email :register-card/password))
(def login-card-t (make :widget/login-card))
(def user-card-t (make :widget/user-card :user-card/user :user-card/data))

(defn sentence [data]
  [(merge {:widget/type :widget/sentence}  data)])

(defn card
  ([data]
   (card -1 data))
  ([id data]
   [(merge (widget id :widget/card)
           {:card/title "New Card from Datomic"
            :card/content  (map :db/id data)
            :card/question (:db/id (first data))
            :card/answer   (:db/id (second data))})
    (merge {:widget/type :widget/sentence} (first data))
    (merge {:widget/type :widget/sentence} (second data))]))

(defn user-card-template
  ([id user]
   {:db/id id
    :widget/type :widget/user-card
    :user-card/user (first user)})
  ([id user data]
   {:db/id id
    :widget/type :widget/user-card
    :user-card/user user
    :user-card/data data}))

(defn make-users [datas]
  [(user-card-template -1 (first datas))
   (user-card-template -2 (second datas))
   {:db/id 19
    :grid/content [-1 -2]}])
