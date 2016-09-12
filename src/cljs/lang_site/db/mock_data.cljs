(ns lang-site.db.mock-data
  (:require
   [cljs.spec :as s]
   [lang-site.components.templates :as t]))

(def card [:widget/card :card/title :card/question :card/answer])
(def grid [:widget/grid :grid/data :grid/content])
(def header [:widget/header :header/title :header/content])
(def header-drawer [:widget/header-drawer :header-drawer/title :header-drawer/content])
(def link [:widget/link :link/text :link/icon :link/href])
(def login-card [:widget/login-card])
(def menu-item [:widget/menu-item :menu-item/text])
(def page [:widget/page :page/content])
(def register-card [:widget/register-card :register-card/user
                    :register-card/email :register-card/password])
(def sentence [:widget/sentence :sentence/text :sentence/group :sentence/language])
(def user [:widget/user :user/name :user/email :user/password])
(def user-card [:widget/user-card :user-card/user :user-card/data])

(defmulti widget :widget/type)
(s/def :widget/user string?)
(s/def :widget/email string?)
(s/def :widget/password string?)
(s/def :widget/shared (s/keys :req [:widget/type]))
(s/def :widget/ref (s/or :expanded :widget/widget :collapsed int?))
(s/def :widget/refs (s/+ :widget/ref))
(s/def :widget/content (s/+ :widget/ref))
(s/def :card/title string?)
(s/def :card/question :widget/ref)
(s/def :card/answer :widget/ref)
(defmethod widget :widget/card [_]
  (s/keys :req [:widget/type :card/title :card/question :card/answer]))

(s/def :grid/data string?)
(s/def :grid/content :widget/refs)
(defmethod widget :widget/grid [_]
  (s/keys :req [:widget/type :grid/content :grid/data]))

(s/def :header/title string?)
(s/def :header/content :widget/refs)
(defmethod widget :widget/header [_]
  (s/keys :req [:widget/type :header/title :header/content]))

(s/def :header-drawer/title string?)
(s/def :header-drawer/content :widget/refs)
(defmethod widget :widget/header-drawer [_]
  (s/keys :req [:widget/type :header-drawer/title :header-drawer/content]))

(s/def :link/text string?)
(s/def :link/icon string?)
(s/def :link/href string?)
(defmethod widget :widget/link [_]
  (s/keys :req [:widget/type :link/text :link/icon :link/href]))

(defmethod widget :widget/login-card [_]
  :widget/shared)

(s/def :menu-item/text string?)
(defmethod widget :widget/menu-item [_]
  (s/keys :req [:widget/type :menu-item/text]))

(s/def :page/content :widget/refs)
(defmethod widget :widget/page [_]
  (s/keys :req [:widget/type :page/content]))

(s/def :register-card/user :widget/user)
(s/def :register-card/email :widget/email)
(s/def :register-card/password :widget/password)
(defmethod widget :widget/register-card [_]
  (s/keys :req [:widget/type
                :register-card/user
                :register-card/email
                :register-card/password]))

(s/def :sentence/text string?)
(s/def :sentence/group int?)
(s/def :sentence/language #{:sentence.language/eng :sentence.language/tur})
(defmethod widget :widget/sentence [_]
  (s/keys :req [:widget/type :sentence/text :sentence/group :sentence/language]))

(s/def :user/name :widget/user)
(s/def :user/email :widget/email)
(s/def :user/password :widget/password)
(defmethod widget :widget/user [_]
  (s/keys :req [:widget/type :user/name :user/email :user/password]))

(s/def :user-card/user :widget/ref)
(s/def :user-card/data :widget/ref)
(defmethod widget :widget/user-card [_]
  (s/keys :req [:widget/type :user-card/user :user-card/data]))
(s/def :widget/widget (s/multi-spec widget :widget/type))

(def many  {:db/cardinality :db.cardinality/many})
(def ref   {:db/valueType :db.type/ref})
(def com   {:db/isComponent true})
(def index {:db/index true})

(def schema
  {:header/content        (merge many ref com)
   :header-drawer/content (merge many ref com)
   :card/question         (merge      ref com)
   :card/answer           (merge      ref com)
   :card/content          (merge many ref com)
   :cloze-card/question   (merge      ref com)
   :close-card/answer     (merge      ref com)
   :grid/content          (merge many ref com)
   :page/content          (merge many ref com)
   :user-card/user        (merge      ref com)
   :register-card/temp    (merge      ref com)
   :widget/type           {:db/index true}})

(def fixtures
  [((apply t/make sentence)       -1 "First Sentence\n" 100 :sentence.language/eng)
   ((apply t/make sentence)       -2 "Second Sentence\n" 100 :sentence.language/tur)
   ((apply t/make header)         -4 "Language test site" [-15 -16 -17])
   ((apply t/make menu-item)     -15 "About")
   ((apply t/make menu-item)     -16 "Contact")
   ((apply t/make menu-item)     -17 "Legal Information")
   ((apply t/make link)          -23 "User" "user" "#user")
   ((apply t/make link)           -5 "Home" "home" "#")
   ((apply t/make link)           -6 "Inbox" "inbox" "#inbox")
   ((apply t/make link)           -7 "Delete" "delete" "#delete")
   ((apply t/make link)          -18 "Report" "report" "#report")
   ((apply t/make link)          -19 "Forum" "forum" "#forum")
   ((apply t/make link)          -20 "Flag" "flag" "#flag")
   ((apply t/make link)          -21 "Promos" "local_offer" "#local_offer")
   ((apply t/make link)          -22 "Bob" "bob@email.com" "password")
   ((apply t/make user-card)     -24 -22 -22)
 #_((apply t/make login-card)    -23)
   ((apply t/make header-drawer)  -8 "Language test site" [-5 -6 -7 -18 -19 -20])
   ((apply t/make grid)           -9 "Placeholder" [-12 -14 -24])
   ((apply t/make page)          -10 [-4 -8 -9])
   ((apply t/make register-card) -14 "" "" "")
   ((apply t/make card)          -12 "Card 1" -1 -2)
   ((apply t/make card)          -13 "Card 2" -1 -2)])
