(ns lang-site.db.mock-data
  (:require
   [lang-site.components.templates :as t]))

(def card [:widget/card :card/title :card/question :card/answer])
(def footer [:widget/footer :footer/left-links :footer/right-links])
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
   :footer/left-links     (merge many ref com)
   :footer/right-links    (merge many ref com)
   :grid/content          (merge many ref com)
   :page/content          (merge many ref com)
   :user-card/user        (merge      ref com)
   :register-card/temp    (merge      ref com)
   :widget/type           {:db/index true}})

(defn make [[type & keys] id & data]
  (merge {:db/id id
          :widget/type type}
         (zipmap keys data)))

(def fixtures
  (mapv #(apply make %)
        [[sentence -1 "First Sentence\n" 100 {:db/ident :sentence.language/eng}]
         [sentence -2 "Second Sentence\n" 100 {:db/ident :sentence.language/tur}]
         [sentence -3 "Third Sentence\n" 101 {:db/ident :sentence.language/eng}]
         [sentence -30 "Fourth Sentence\n" 102 {:db/ident :sentence.language/tur}]
         [header -4 "Language test site" [-15 -16 -17]]
         [menu-item -15 "About"]
         [menu-item -16 "Contact"]
         [menu-item -17 "Legal Information"]
         [link -23 "User" "user" "#user"]
         [link -5 "Home" "home" "#"]
         [link -6 "Inbox" "inbox" "#inbox"]
         [link -7 "Delete" "delete" "#delete"]
         [link -18 "Report" "report" "#report"]
         [link -19 "Forum" "forum" "#forum"]
         [link -20 "Flag" "flag" "#flag"]
         [link -21 "Promos" "local_offer" "#local_offer"]
         [user -22 "Bob" "bob@email.com" "password"]
         [user-card -24 -22 -22]
         [header-drawer -8 "Language test site" [-5 -6 -7 -18 -19 -20]]
         [grid -9 "Placeholder" [-12 -13 -14 -24]]
         [page -10 [-4 -8 -9 -31]]
         [register-card -14 "User" "Example@example.com" "ExamplePassword"]
         [card -12 "Card 1" -1 -2]
         [card -13 "Card 2" -3 -30]
         [footer -31 [-18 -20] [-23 -21]]]))
