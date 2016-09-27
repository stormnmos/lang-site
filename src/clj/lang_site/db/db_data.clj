(ns lang-site.db.db-data
  (:require
   [lang-site.util :as u]
   [lang-site.db.transaction-templates :as tt]
   [lang-site.db.queries :as q]
   [lang-site.state :refer [conn tx-chan]]
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

(def rdr-s (clojure.java.io/reader (env :sentence-file)))
(def rdr-l (clojure.java.io/reader (env :links-file)))
#_(def rdr-t (clojure.java.io/reader (env :tags-file)))

(def schema-tx (read-string (slurp "/home/storm/clojure/lang-site/resources/data/lang-site-schema.edn")))

(defn tag->datomic [line]
  (->> line
       (split-by-tab)))

(defn sentence-id->db-id [id]
  (q/pba-e :sentence/id (read-string id)))

(defn sentence-ids->db-ids [ids]
  (map #(q/pba-e :sentence/id (read-string %)) ids))

(defn process-link-line [line]
  (->> line
       (split-by-tab)
       (sentence-ids->db-ids)
       (filter int?)
       (map (partial d/entity (d/db conn)))
       (mapv d/touch)))

(defn entities->link [entities n]
  (->> n
       (nth entities)
       ((juxt :db/id :sentence/group))
       (apply tt/link)))

(defn link-to-datomic [line]
  (let [entities (process-link-line line)]
    (->> (case (->> entities (map :sentence/group) (filter true?) count)
           0 (->> entities (map :db/id) (tt/links-template))
           1 (if (->> entities second :sentence/group)
               (entities->link entities 0) (entities->link entities 1))
           2 (when (not= (-> entities (nth 0) :sentence/group)
                         (-> entities (nth 1) :sentence/group))
               (->> entities second :sentence/group (q/pba-es :sentence/group)
                    (tt/change-sentence-group (:sentence/group (first entities))))))
         (async/>!! tx-chan))))

(defn sentence-to-datomic [line]
  (->> line
       (split-by-tab)
       (tt/sentence)
       (async/>!! tx-chan)))

(defn transact-links []
  (run! link-to-datomic (line-seq rdr-l)))

(defn transact-sentences []
  (run! sentence-to-datomic (line-seq rdr-s)))
