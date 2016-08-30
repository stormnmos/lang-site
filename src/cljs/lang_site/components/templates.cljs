(ns lang-site.components.templates)

(defn sentence [data]
  [(merge {:widget/type :sentence}  data)])

(defn card
  ([data]
   (card -1 data))
  ([id data]
   [{:db/id id
     :widget/type :card
     :card/title "New Card from Datomic"
     :card/content  (map :db/id data)}
    (merge {:widget/type :sentence} (first data))
    (merge {:widget/type :sentence} (second data))]))

(defn card-template [id title sentence-eids]
  {:db/id id
   :widget/type :card
   :card/title title
   :card/content sentence-eids})

(defn cloze-card-template [id title question answer]
  {:db/id id
   :widget/type :cloze-card
   :cloze-card/title title
   :cloze-card/question question
   :cloze-card/answer answer})

(defn register-card-template [id]
  {:db/id id
   :widget/type :register-card})

(defn login-card-template [id]
  {:db/id id
   :widget/type :login-card})

(defn sentence-template [id text group lang]
  {:db/id id
   :widget/type :sentence
   :sentence/text text
   :sentence/group group
   :sentence/language lang})

(defn link-template [id text icon href]
  {:db/id id
   :widget/type :link
   :link/text text
   :link/icon icon
   :link/href href})

(defn user-template [id name email]
  {:db/id id
   :widget/type :user
   :user/name name
   :user/email email})

(defn user-card-template
  ([data]
   {:db/id -1
      :widget/type :user-card
      :user-card/user (first data)})
  ([id user]
   (user-card-template id user nil))
  ([id user data]
   {:db/id id
    :widget/type :user-card
    :user-card/user user
    :user-card/data data}))

(defn make-users [datas]
  (mapv user-card-template datas))

(defn header-template [id title content-eids]
  {:db/id id
   :widget/type :header
   :header/title title
   :header/content content-eids})

(defn header-drawer-template [id title content]
  {:db/id id
   :widget/type :header-drawer
   :header-drawer/title title
   :header-drawer/content content})

(defn menu-item-template [id text]
  {:db/id id
   :widget/type :menu-item
   :menu-item/text text})

(defn grid-template [id data components]
  {:db/id id
   :widget/type :grid
   :grid/data data
   :grid/content components})

(defn page-template [id content]
  {:db/id id
   :widget/type :page
   :page/content content})
