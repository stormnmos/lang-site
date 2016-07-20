(ns lang-site.db.db-data
  (:require [clojure-csv.core :as csv]
            [datomic.api :as d]
            [clojure.string :as str :only split]
            [clojure.core.async :as async :refer [<! >! >!! put! take!]]))

(defn split-by-tab [s]
  (str/split s #"\t"))

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

(defn create-lang-db []
  (d/create-database uri))

(def conn (d/connect uri))

(async/go
  (while true
    (let [tx (<! events)]
      (ingest conn tx))))

(def rdr-s (clojure.java.io/reader "resources/data/sentences.csv"))

(def rdr-l (clojure.java.io/reader "resources/data/links.csv"))

(def schema-tx (read-string (slurp "resources/data/lang-site-schema.edn")))

(defn load-schema [conn]
  @(d/transact conn schema-tx))

(comment (d/transact
          conn
          (take 100000
                (map :tx
                     (map links-template
                          (keep identity
                                (map
                                 (comp #(find-translation-pair (d/db conn) %)
                                       split-by-tab)
                                 (line-seq rdr-l))))))))

#_ (defn read-links
     (let f (comp #(map tx %)
                  #(map links-template %)
                  #(keep identity %)
                  #(map
                    (comp)))))

(defn find-translation-pair [db [s-id t-id]]
  (d/q '[:find [?e1 ?e2]
         :in $ ?sentence-id ?translation-id
         :where [?e1 :sentence/id ?sentence-id]
                [?e2 :sentence/id ?translation-id]]
       db  (read-string s-id) (read-string t-id)))

(defn links-template [[sentence-id translation-id]]
  {:type :link
   :tx
   {:db/id #db/id[:db.part/user]
    :translation/group [sentence-id translation-id]}})

(defn link-to-datomic [db line]
  (let [vals (split-by-tab line)
        ids  (find-translation-pair db vals)]
    (if (= (count ids) 2)
      (->> ids
           (links-template)
           (put! events)))))

(defn get-links [db]
  (keep identity
        (map (comp #(find-translation-pair db %) split-by-tab)
             (line-seq rdr-l))))

(defn sentence-template [[id lang text]]
  {:type :sentence
   :tx
   {:db/id #db/id[:db.part/user] :sentence/id (read-string id)
    :sentence/language
    (cond
      (= "eng" lang) :sentence.language/eng
      (= "tur" lang) :sentence.language/tur)
    :sentence/text text}})

(defn sentence-to-datomic [sent]
  (->> sent
       (split-by-tab)
       (sentence-template)
       (put! events)))

(defmulti validate-tx
  "Ingest a transaction into Datomic DB"
  :type)

(defmethod validate-tx :sentence [conn {{lang :sentence/language :as tx} :tx}]
  (if (or (= lang :sentence.language/eng) ; only support tur and english
          (= lang :sentence.language/tur))
    tx))

(defmethod validate :link [conn {{group :translation/group :as tx} :tx}]
  (if-not (some true?
                (d/pull-many (d/db conn) [:translation_/group] tx))
    tx))

(defn process-transactions [conn]
  (async/go
    (while true
      (let [tx (<! events)]
        (d/transact conn (validate-tx tx))))))

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

;; Sample ENG<->TUR pair
;; English 3898641 17592187516140, Turkish 3949090, 17592187570252
;; ["3898641" "3949090"] "3898641\t3949090"
;; 17592186156439, 170564; 17592186168085, 243919
;; "170564\t243919"

(defn pull-by-sentence [db sentence]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?sentence
         :where [?e :sentence/text ?sentence]]
       db sentence))
