(ns lang-site.db.db
  (:require
   [lang-site.db.db-data :as dbd]
   [lang-site.db.queries :as q]
   [lang-site.db.transaction-templates :as tt]
   [datomic.api :as d]
   [environ.core :refer [env]]
   [clojure.core.async
    :as async :refer [chan sliding-buffer
                      <! >! <!!
                      >!! put! take!]]))

(def transactions (chan 100))

(defn get-transaction-channel []
  transactions)

(def failed-response  (chan (sliding-buffer 10)))
(def success-response (chan (sliding-buffer 10)))

(def uri (env :database-url))
#_(def schema-tx (read-string (slurp "resources/data/lang-site-schema.edn")))

(defn load-schema [conn schema-tx]
  @(d/transact conn schema-tx))

(defn create-db-connection [uri]
  (d/connect uri))

(defn create-db-access-function [conn]
  (fn [] (d/db conn)))

(def get-db (create-db-access-function (create-db-connection uri)))

(def conn (d/connect uri))

(defmulti validate-tx
  "Ingest a transaction into Datomic DB"
  :type)

(defmethod validate-tx :sentence [{{lang :sentence/language :as tx} :tx}]
  (if (or (= lang :sentence.language/eng) ; only support tur and english
          (= lang :sentence.language/tur))
    tx))

(defmethod validate-tx :link [{tx :tx}]
  tx)

(async/go
  (while true
    (let [unvalidated-tx (<! transactions)]
      (if-let [tx (validate-tx unvalidated-tx) ]
        (do (d/transact conn tx)
            (>! success-response tx))
        (>! failed-response unvalidated-tx)))))
