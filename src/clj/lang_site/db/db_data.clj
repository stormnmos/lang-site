(ns lang-site.db.db-data
  (:require
   [lang-site.db.db :as lsdb]
   [lang-site.db.transaction-templates :as tt]
   [datomic.api :as d]
   [clojure.string :as str :only split]
   [clojure.core.async
    :as async :refer [chan sliding-buffer
                      <! >! <!!
                      >!! put! take!]]))

(defn split-by-tab [s]
  (str/split s #"\t"))

(defn create-lang-db [uri]
  (d/create-database uri))

(def rdr-s (clojure.java.io/reader "resources/data/sentences.csv"))
(def rdr-l (clojure.java.io/reader "resources/data/links.csv"))

(defn sentence-ids->db-ids [db ids]
  (map #(pba-e db :sentence/id (read-string %)) ids))

(defn process-link-line [db line]
  (->> line
       (split-by-tab)
       (sentence-ids->db-ids db)
       (filter int?)))

(defn link-to-datomic [db line]
  (let [eids (process-link-line db line)]
    (if (>= (count eids) 2)
      (->> eids
           (tt/links-template)
           (put! (lsdb/get-transaction-channel))))))

(defn sentence-to-datomic [sent]
  (->> sent
       (split-by-tab)
       (tt/sentence-template)
       (put! (lsdb/get-transaction-channel))))

(defn transact-links []
  (run! #(link-to-datomic db %) (line-seq rdr-l)))

(defn transact-sentences []
  (run! sentence-to-datomic (line-seq rdr-s)))
