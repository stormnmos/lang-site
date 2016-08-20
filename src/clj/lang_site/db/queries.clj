(ns lang-site.db.queries
  (:require [datomic.api :as d]
            [lang-site.state :refer [conn]]
            [lang-site.util :as u]))

(defn pba-e [att v]
  "Pull by attribute, entity"
  (d/q '[:find ?e .
         :in $ ?att ?v
         :where [?e ?att ?v ]]
       (d/db conn) att v))

(defn pull-by-sentence-id [s-id]
  (d/q '[:find [(pull ?e [:db/id :sentence/group])]
         :in $ ?v
         :where [?e :sentence/id ?v]]
       (d/db conn) s-id))

(defn text-q [search]
  (d/q '[:find ?e
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       (d/db conn) search))

(defn find-links []
  (d/q '[:find ?a
         :where [_ :translation/group ?a]]
       (d/db conn)))

(defn find-links-pull []
  (d/q '[:find (pull ?a [*])
         :where [_ :translation/group ?a]]
       (d/db conn)))

(defn find-fulltext [search-string]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       (d/db conn) search-string))

(defn pull-by-sentence [sentence]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?sentence
         :where [?e :sentence/text ?sentence]]
       (d/db conn) sentence))

(defn sample-language-groups [n]
  (d/q '{:find  [(sample 10 ?v)]
         :in [$ ?n]
         :where [[_ :sentence/group ?v]]}
       (d/db conn) n))

(defn sample-sentence-group-squuid []
  (first (d/q '[:find (sample 1 ?val) .
                :where
                [_ :sentence/group ?val]]
              (d/db conn))))

(defn pull-many-translation-pair
  [n]
  (->> (d/datoms (d/db conn) :avet :sentence/group)
       (map :v)
       (shuffle)
       (take n)))

(defn pull-translation-pair
([]
 (pull-translation-pair
  (->> (d/datoms (d/db conn) :avet :sentence/group)
       (mapv :v)
       (rand-nth))))
  ([squuid]
 (let [result (d/q '[:find [(pull ?e1 [:db/id
                                       :sentence/id
                                       {:sentence/language [:db/ident]}
                                       :sentence/group
                                       :sentence/text])
                            (pull ?e2 [:db/id
                                       :sentence/id
                                       {:sentence/language [:db/ident]}
                                       :sentence/group
                                       :sentence/text])]
                     :in $ ?squuid
                     :where
                     [?e1 :sentence/group ?squuid]
                     [?e1 :sentence/language :sentence.language/eng]
                     [?e2 :sentence/group ?squuid]
                     [?e2 :sentence/language :sentence.language/tur]]
                   (d/db conn) squuid)]
   (if (nil? result)
     (pull-translation-pair)
     result))))

(defn pull-schema []
  (d/q '[:find (pull ?e [*]) .
         :where [:db.part/db :db.install/attribute ?p]
                [?p :db/ident ?e]]
       (d/db conn)))

(defn pull-users []
  (d/q '[:find (pull ?e [*]) .
         :in $
         :where [?e :user/name]]
       (d/db conn)))

(defn pull-user-by-name [name]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?name
         :where [?e :user/name ?name]]
       (d/db conn) name))
