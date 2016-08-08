(ns lang-site.mock-data)

(def schema
  {:translation/group
   {:db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}
   :header/links
   {:db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}
   :header-drawer/links
   {:db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}
   :widget/content
   {:db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}
   :card/sentences
   {:db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}})

(def fixtures
  [{:db/id 0
    :app/title "Hello, DataScript!"
    :app/count 0}
   {:db/id -1
    :widget/type :sentence
    :sentence/text "First Sentence\n"
    :sentence/group 100
    :sentence/language :sentence.language/eng}
   {:db/id -2
    :widget/type :sentence
    :sentence/text "Second Sentence\n"
    :sentence/group 100
    :sentence/language :sentence.language/tur}
   {:db/id -3
    :widget/type :translation
    :translation/group #{-1 -2}}
   {:db/id -4
    :widget/type :header
    :header/title "Language test site"
    :widget/content [-5 -6 -7]}
   {:db/id -5
    :widget/type :link
    :link/text "Link1"}
   {:db/id -6
    :widget/type :link
    :link/text "Link2"}
   {:db/id -7
    :widget/type :link
    :link/text "Link3"}
   {:db/id -8
    :widget/type :header-drawer
    :header-drawer/title "Language test site"
    :widget/content [-5 -6 -7]}
   {:db/id -9
    :widget/type :grid
    :grid/data "Placeholder"}
   {:db/id -100
    :widget/type :page}
   {:db/id -101
    :widget/type :card
    :card/title "Card 1"
    :card/sentences [-1 -2]
    :card/content [-1 -2]}
   {:db/id -102
    :widget/type :card
    :card/title "Card 2"
    :card/sentences [-1 -2]
    :card/content "asdfasdf"}])
