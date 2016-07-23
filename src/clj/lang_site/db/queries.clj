(ns lang-site.db.queries
  (:require [datomic.api :as d]))

(defn pba-e [db att v]
  "Pull by attribute, entity"
  (d/q '[:find ?e .
         :in $ ?att ?v
         :where [?e ?att ?v ]]
       db att v))

(defn pull-by-sentence-id [db s-id]
  (d/q '[:find [(pull ?e [:db/id :sentence/group])]
         :in $ ?v
         :where [?e :sentence/id ?v]]
       db s-id))

(defn text-q [db search]
  (d/q '[:find ?e
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       db search))

(defn find-links [db]
  (d/q '[:find ?a
         :where [_ :translation/group ?a]]
       db))

(defn find-links-pull [db]
  (d/q '[:find (pull ?a [*])
         :where [_ :translation/group ?a]]
       db))

(defn find-fulltext [db search-string]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       db search-string))

(defn pull-by-sentence [db sentence]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?sentence
         :where [?e :sentence/text ?sentence]]
       db sentence))

(defn sample-language-groups [db n]
  (d/q '{:find  [(sample 10 ?v)]
         :in [$ ?n]
         :where [[_ :sentence/group ?v]]}
       db n))

(defn sample-sentence-group-squuid [db]
  (first (d/q '[:find (sample 1 ?val) .
                :where [_ :sentence/group ?val]]
              db)))

(defn pull-translation-pair
  ([db]
   (pull-translation-pair db (sample-sentence-group-squuid db)))
  ([db squuid]
   (d/q '[:find ?sentence-text
          :in $ ?squuid
          :where [?e :sentence/group ?squuid]
                 [?e :sentence/text  ?sentence-text]]
        db squuid)))
