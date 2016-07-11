(ns lang-site.db.db-data
  (:require [clojure-csv.core :as csv]
            [datomic.api :as d]))

(defn lazy-file-reader [filename]
  (fn [f]
    (with-open [rdr (clojure.java.io/reader filename)]
      (doall (map f (line-seq rdr))))))

(def uri "datomic:couchbase://localhost:4334/datomic/lang-site/?password=password")

(d/create-database uri)

(def conn (d/connect uri))

(def schema-tx (read-string (slurp "resources/data/lang-site-schema.edn")))

@(d/transact conn schema-tx)


(defn sentence-template [[id lang text]]
  {:db/id #db/id[:db.part/user] :sentence/id (read-string id)
   :sentence/language lang :sentence/text text})

(defn sentence-to-datomic [sent]
  (as-> sent s
    (csv/parse-csv s :delimiter \tab)
    (first s)
    (sentence-template s)
    (d/transact conn [s])))
