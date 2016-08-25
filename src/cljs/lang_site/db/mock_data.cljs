(ns lang-site.db.mock-data
  (:require [lang-site.components.templates :as t]))

(def component {:db/cardinality :db.cardinality/many
                :db/valueType   :db.type/ref
                :db/isComponent true})

(def schema
  {:header/content        component
   :header-drawer/content component
   :card/content          component
   :grid/content          component
   :page/content          component
   :widget/type           {:db/index true}})

(def fixtures
  [(t/sentence-template -1 "First Sentence\n" 100 :sentence.language/eng)
   (t/sentence-template -2 "Second Sentence\n" 100 :sentence.language/tur)
   (t/header-template -4 "Language test site" [-5 -6 -7])
   (t/link-template -5 "Link1")
   (t/link-template -6 "Link2")
   (t/link-template -7 "Link3")
   (t/header-drawer-template -8 "Language test site" [-5 -6 -7])
   (t/grid-template -9 "Placeholder" [-12])
   (t/page-template -10 [-4 -8 -9 -14])
   (t/card-template -12 "Card 1" [-1 -2])
   (t/card-template -13 "Card 2" [-1 -2])
   (t/register-card-template -14)])
