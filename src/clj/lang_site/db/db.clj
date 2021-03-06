(ns lang-site.db.db
  (:require
   [lang-site.db.db-data :as dbd]
   [lang-site.db.queries :as q]
   [lang-site.db.transaction-templates :as tt]
   [lang-site.state :refer [conn]]
   [datomic.api :as d]
   [environ.core :refer [env]]
   [clojure.core.async
    :as async :refer [chan sliding-buffer
                      <! >! <!!
                      >!! put! take!]]))

(defn load-schema [schema-tx]
  (d/transact conn schema-tx))

(defmulti validate-tx
  "Ingest a transaction into Datomic DB"
  :type)

(defmethod validate-tx :sentence [{{lang :sentence/language :as tx} :tx}]
  #_(if (or (= lang :sentence.language/eng) ; only support tur and english
          (= lang :sentence.language/tur))
      tx)
  tx)

(defmethod validate-tx :link [{tx :tx}]
  tx)
