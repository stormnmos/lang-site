(ns lang-site.db.db-data
  (:require [clojure-csv.core :as csv]
            [datomic.api :as d]
            [clojure.string :as str :only split]
            [clojure.core.async :as async :refer [chan sliding-buffer <! >! <!! >!! put! take!]]))

(defn split-by-tab [s]
  (str/split s #"\t"))

(def events (chan 100000))

(def failed-response  (chan (sliding-buffer 10)))
(def success-response (chan (sliding-buffer 10)))

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
(def db (d/db conn))

(def rdr-s (clojure.java.io/reader "resources/data/sentences.csv"))

(def rdr-l (clojure.java.io/reader "resources/data/links.csv"))

(def schema-tx (read-string (slurp "resources/data/lang-site-schema.edn")))

(defn load-schema [conn]
  @(d/transact conn schema-tx))

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

(defn sentence-ids->db-ids [db ids]
  (map #(pba-e db :sentence/id (read-string %)) ids))

(defn links-template [eids]
  (let [squuid (d/squuid)]
    {:type :link
     :tx (mapv (fn [eid]
                 {:db/id eid
                  :sentence/group squuid})
               eids)}))

(defn process-link-line [db line]
  (->> line
       (split-by-tab)
       (sentence-ids->db-ids db)
       (filter int?)))

(defn link-to-datomic [db line]
  (let [eids (process-link-line db line)]
    (if (>= (count eids) 2)
      (->> eids
           (links-template)
           (put! events)))))

(defn get-links [db]
  (keep identity
        (map (comp #(find-translation-pair db %) split-by-tab)
             (line-seq rdr-l))))

(defn sentence-template [[id lang text]]
  {:type :sentence
   :tx
   [{:db/id #db/id[:db.part/user] :sentence/id (read-string id)
     :sentence/language
     (cond
       (= "eng" lang) :sentence.language/eng
       (= "tur" lang) :sentence.language/tur)
     :sentence/text text}]})

(defn sentence-to-datomic [sent]
  (->> sent
       (split-by-tab)
       (sentence-template)
       (put! events)))

(defmulti validate-tx
  "Ingest a transaction into Datomic DB"
  :type)

(defmethod validate-tx :sentence [{{lang :sentence/language :as tx} :tx}]
  (if (or (= lang :sentence.language/eng) ; only support tur and english
          (= lang :sentence.language/tur))
    tx))

(defmethod validate-tx :link [{tx :tx}]
  tx)

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

(defn transact-links []
  (run! #(link-to-datomic db %) (line-seq rdr-l)))

(defn transact-sentences []
  (run! sentence-to-datomic (line-seq rdr-s)))

(async/go
  (while true
    (let [unvalidated-tx (<! events)]
      (if-let [tx (validate-tx unvalidated-tx) ]
        (do (d/transact conn tx)
            (>! success-response tx))
        (>! failed-response unvalidated-tx)))))
