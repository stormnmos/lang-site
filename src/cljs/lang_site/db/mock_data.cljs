(ns lang-site.db.mock-data
  (:require [lang-site.components.templates :as t]))

(def many {:db/cardinality :db.cardinality/many})
(def ref  {:db/valueType :db.type/ref})
(def com  {:db/isComponent true})

(def schema
  {:header/content        (merge many ref com)
   :header-drawer/content (merge many ref com)
   :card/content          (merge many ref com)
   :cloze-card/question   (merge      ref com)
   :close-card/answer     (merge      ref com)
   :grid/content          (merge many ref com)
   :page/content          (merge many ref com)
   :user-card/user        (merge      ref com)
   :widget/type           {:db/index true}})

(def fixtures
  [(t/sentence-template -1 "First Sentence\n" 100 :sentence.language/eng)
   (t/sentence-template -2 "Second Sentence\n" 100 :sentence.language/tur)
   (t/header-template -4 "Language test site" [-15 -16 -17])
   (t/menu-item-template -15 "About")
   (t/menu-item-template -16 "Contact")
   (t/menu-item-template -17 "Legal Information")
   (t/link-template -23 "User" "user" "#user")
   (t/link-template -5 "Home" "home" "#")
   (t/link-template -6 "Inbox" "inbox" "#inbox")
   (t/link-template -7 "Delete" "delete" "#delete")
   (t/link-template -18 "Report" "report" "#report")
   (t/link-template -19 "Forum" "forum" "#forum")
   (t/link-template -20 "Flag" "flag" "#flag")
   (t/link-template -21 "Promos" "local_offer" "#local_offer")
   (t/user-template -22 "Bob" "bob@email.com")
   (t/user-card-template -24 -22)
   (t/login-card-template -23)
   (t/cloze-card-template -25 "Cloze" -1 -2)
   (t/header-drawer-template -8 "Language test site" [-5 -6 -7 -18 -19 -20])
   (t/grid-template -9 "Placeholder" [-12 -14])
   (t/page-template -10 [-4 -8 -9])
   (t/card-template -12 "Card 1" [-1 -2])
   (t/card-template -13 "Card 2" [-1 -2])
   (t/register-card-template -14)])
