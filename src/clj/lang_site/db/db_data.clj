(ns lang-site.db.db-data
  (:require
   [lang-site.util :as u]
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

#_(def rdr-s (clojure.java.io/reader (env :sentence-file)))
#_(def rdr-l (clojure.java.io/reader (env :links-file)))

(defn sentence-ids->db-ids [state ids]
  (map #(q/pba-e (u/get-db state) :sentence/id (read-string %)) ids))

(defn process-link-line [state line]
  (->> line
       (split-by-tab)
       (sentence-ids->db-ids state)
       (filter int?)))

(defn link-to-datomic [state tx-chan line]
  (let [eids (process-link-line state line)]
    (if (>= (count eids) 2)
      (->> eids
           (tt/links-template)
           (put! tx-chan)))))

(defn sentence-to-datomic [state line]
  (->> line
       (split-by-tab)
       (tt/sentence-template)
       (put! (:tx-chan state))))

#_ (defn transact-links [state]
  (run! #(link-to-datomic state %) (line-seq rdr-l)))

#_ (defn transact-sentences []
  (run! sentence-to-datomic (line-seq rdr-s)))
