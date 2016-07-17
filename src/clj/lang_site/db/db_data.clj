(ns lang-site.db.db-data
  (:require [clojure-csv.core :as csv]
            [datomic.api :as d]
            [clojure.string :as str :only split]
            [clojure.core.async :as async :refer [<! >! >!! put! take!]]))

(def events (async/chan 1000000))

(def event-bus (async/chan 1000))

(def event-bus-pub (async/pub event-bus first))

(defn send-msg [chan text]
  (async/put! chan [:send-msg text]))

(defn lazy-file-reader [filename]
  (fn [f]
    (with-open [rdr (clojure.java.io/reader filename)]
      (doall (map f (line-seq rdr))))))

(def uri "datomic:couchbase://localhost:4334/datomic/lang-site/?password=password")

(d/create-database uri)

(def conn (d/connect uri))

#_(def rdr-s (clojure.java.io/reader "resources/data/sentences.csv"))

(def rdr-l (clojure.java.io/reader "resources/data/links.csv"))

(def schema-tx (read-string (slurp "resources/data/lang-site-schema.edn")))

@(d/transact conn schema-tx)

(defn find-translation-pair [db [s-id t-id]]
  (d/q '[:find [?e1 ?e2]
         :in $ ?sentence-id ?translation-id
         :where [?e1 :sentence/id ?sentence-id]
                [?e2 :sentence/id ?translation-id]]
       db s-id t-id))

#_(defn links-template [[sentence-id translation-id]]
  {:db/id #db/id[:db.part/user]
   :sentence/id sentence-id
   :sentence/translation translation-id})

(defn links-template [[sentence-id translation-id]]
  {:db/id #db/id[:db.part/user]
   :translation/group [sentence-id translation-id]})

(defn link-to-datomic [line]
  (let [vals (str/split line #"\t")
        ids  (find-translation-pair (d/db conn) vals)]
    (if (= (count ids) 2)
      (->> ids
          (links-template)
          (put! events)))))

(defn sentence-template [[id lang text]]
  {:db/id #db/id[:db.part/user] :sentence/id (read-string id)
   :sentence/language
   (cond
     (= "eng" lang) :sentence.language/eng
     (= "tur" lang) :sentence.language/tur)
   :sentence/text text})

(defn sentence-to-datomic [sent]
  (as-> sent s
    (str/split s #"\t")
    (sentence-template s)
    (put! events s)))

(async/go
  (while true
    (let [tx (<! events)]
      (if (or (= (:sentence/language tx) :sentence.language/eng)
              (= (:sentence/language tx) :sentence.language/tur))
        (d/transact conn [tx])))))

#_(async/go
  (while true
    (let [tx (<! events)]
      (if (:translation/group tx)
        (if-not (some true?
                      (d/pull-many (d/db conn)
                                   [:translation_/group]
                                   (:translation/group tx)))
          (d/transact conn [tx]))))))

(defn excise-template [tx-id]
  [{:db/id #db/id[:db.part/user]
    :db/excise tx-id}])

(defn find-duplicate-values-entids-datalog [db attr-name]
  (->> (d/q '[:find ?e ?v
              :in $ ?a
              :where [?e ?a ?v]]
            db attr-name)
       (group-by second)
       (filter #(> (count (second %)) 1))
       (mapcat second)
       (map first)))

(defn text-q [db search]
  (d/q '[:find ?e
         :in $ ?search
         :where [(fulltext $ :sentence/text ?search) [[?e]]]]
       db search))
