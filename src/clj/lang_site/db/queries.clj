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
