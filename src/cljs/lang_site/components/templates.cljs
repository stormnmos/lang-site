(ns lang-site.components.templates
  (:require [cljs.spec :as s]))

(defn widget [id type]
  {:db/id id
   :widget/type type})

(defn make [type & keys]
  (fn [id & data]
    (merge (widget id type)
           (zipmap keys data))))

(defn sentence [data]
  [(merge {:widget/type :widget/sentence} data)])

(defn card
  ([data]
   (card -1 data))
  ([id data]
   [(merge (widget id :widget/card)
           {:card/title "New Card from Datomic"
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
