(ns lang-site.db.queries
  (:require [datomic.api :as d]
            [lang-site.util :as u]))

(defn pba-e [state att v]
  "Pull by attribute, entity"
  (d/q '[:find ?e .
         :in $ ?att ?v
         :where [?e ?att ?v ]]
       (u/get-db state) att v))

(defn pull-by-sentence-id [state s-id]
  (d/q '[:find [(pull ?e [:db/id :sentence/group])]
         :in $ ?v
         :where [?e :sentence/id ?v]]
       (u/get-db state) s-id))

(defn text-q [state search]
  (d/q '[:find ?e
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       (u/get-db state) search))

(defn find-links [state]
  (d/q '[:find ?a
         :where [_ :translation/group ?a]]
       (u/get-db state)))

(defn find-links-pull [state]
  (d/q '[:find (pull ?a [*])
         :where [_ :translation/group ?a]]
       (u/get-db state)))

(defn find-fulltext [state search-string]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       (u/get-db state) search-string))

(defn pull-by-sentence [state sentence]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?sentence
         :where [?e :sentence/text ?sentence]]
       (u/get-db state) sentence))

(defn sample-language-groups [state n]
  (d/q '{:find  [(sample 10 ?v)]
         :in [$ ?n]
         :where [[_ :sentence/group ?v]]}
       (u/get-db state) n))

(defn sample-sentence-group-squuid [state]
  (first (d/q '[:find (sample 1 ?val) .
                :where [_ :sentence/group ?val]]
              (u/get-db state))))



(defn pull-translation-pair*
  ([state]
   (pull-translation-pair state (sample-sentence-group-squuid state)))
  ([state squuid]
   (d/q '[:find [?text1 ?text2]
          :in $ ?squuid
          :where
          [?e1 :sentence/group ?squuid]
          [?e1 :sentence/text  ?text1]
          [?e2 :sentence/group ?squuid]
          [?e2 :sentence/text  ?text2]]
        (u/get-db state) squuid)))

(defn pull-translation-pair
([state]
(pull-translation-pair state (sample-sentence-group-squuid state)))
([state squuid]
 (d/q '[:find [(pull ?e1 [*]) (pull ?e2 [*])]
       :in $ ?squuid
       :where
       [?e1 :sentence/group ?squuid]
       [?e1 :sentence/language :sentence.language/eng]
       [?e2 :sentence/group ?squuid]
       [?e2 :sentence/language :sentence.language/tur]]
(u/get-db state) squuid)))

(defn pull-schema [state]
  (d/q '[:find (pull ?e [*]) .
         :where [:db.part/db :db.install/attribute ?p]
                [?p :db/ident ?e]]
       (u/get-db state)))
