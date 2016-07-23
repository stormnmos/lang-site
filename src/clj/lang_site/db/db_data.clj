(ns lang-site.db.db-data
  (:require
   [lang-site.db.transaction-templates :as tt]
   [lang-site.db.queries :as q]
   [datomic.api :as d]
   [environ.core :refer [env]]
   [clojure.string :as str :only split]
   [clojure.core.async
    :as async :refer [chan sliding-buffer
                      <! >! <!!
                      >!! put! take!]]
   [environ.core :refer [env]]))

(defn split-by-tab [s]
  (str/split s #"\t"))

(defn create-lang-db [uri]
  (d/create-database uri))

(def rdr-s (clojure.java.io/reader (env :sentence-file)))
(def rdr-l (clojure.java.io/reader (env :links-file)))

(defn sentence-ids->db-ids [db ids]
  (map #(q/pba-e db :sentence/id (read-string %)) ids))

(defn process-link-line [db line]
  (->> line
       (split-by-tab)
       (sentence-ids->db-ids db)
       (filter int?)))

(defn link-to-datomic [db tx-chan line]
  (let [eids (process-link-line db line)]
    (if (>= (count eids) 2)
      (->> eids
           (tt/links-template)
           (put! tx-chan)))))

(defn sentence-to-datomic [tx-chan line]
  (->> line
       (split-by-tab)
       (tt/sentence-template)
       (put! tx-chan)))

(defn transact-links [db]
  (run! #(link-to-datomic db %) (line-seq rdr-l)))

(defn transact-sentences []
  (run! sentence-to-datomic (line-seq rdr-s)))
