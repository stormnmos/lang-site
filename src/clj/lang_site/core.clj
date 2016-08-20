(ns lang-site.core
  (:require
   [clojure.edn :as edn]
   [clojure.core.async :as async]
   [datomic.api :as d]
   [environ.core :refer [env]]
   [lang-site.db.db :as db]
   [lang-site.db.queries :as q]
   [lang-site.handler :as h]
   [lang-site.state :as state :refer [conn tx-chan fail-chan success-chan]]
   [lang-site.util :as u]
   [mount.core :as mount]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.transit :as transit :only [wrap-transit-body
                                               wrap-transit]]))

(mount/start)

(def handler
  (-> h/routes
      (transit/wrap-transit-response {:encoding :json})
      (wrap-keyword-params)
      (wrap-params)
      (wrap-session)))

;;; Main handler for transacting into datomic
(async/go
  (while true
    (let [unvalidated-tx (async/<! tx-chan)]
      (if-let [tx (db/validate-tx unvalidated-tx) ]
        (d/transact conn tx)
        (async/>! fail-chan unvalidated-tx)))))
