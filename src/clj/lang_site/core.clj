(ns lang-site.core
  (:require [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [clojure.core.async :as async]
            [datomic.api :as d]
            [environ.core :refer [env]]
            [lang-site.db.db :as db]
            [lang-site.db.queries :as q]
            [lang-site.state :as state]
            [lang-site.util :as u]
            [ring.middleware.transit :as transit :only [wrap-transit-body
                                                        wrap-transit]]
            [com.stuartsierra.component :as component]))

(def state (->> (state/new-state
                 (env :database-url)
                 (async/chan 100)
                 (async/chan (async/sliding-buffer 10))
                 (async/chan (async/sliding-buffer 10)))
                (component/start)))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/text"}
   :body (pr-str data)})

(defroutes routes
  (GET "/translation-group" []
       {:status 200
        :body  (q/pull-translation-pair state)})
  (GET "/translation-group/:squuid" [squuid]
       (pr-str (q/pull-translation-pair state squuid)))
  (GET "/api/echo" request
       {:status 200
        :headers {"Content-Type" "application/transit"}
        :body request})
  (GET "/api/schema" []
       {:status 200
        :body (q/pull-schema state)})
  (route/files "/" {:root "target"})
  (route/not-found "<h1>Page not found</h1>"))

(def handler
  (transit/wrap-transit-body routes {:encoding :json}))

;;; Main handler for transacting into datomic
(async/go
  (while true
    (let [{:keys [tx-chan fail-chan success-chan]} state
          unvalidated-tx (async/<! tx-chan)]
      (if-let [tx (db/validate-tx unvalidated-tx) ]
        (do (d/transact (:connection state) tx)
            (async/>! success-chan tx))
        (async/>! fail-chan unvalidated-tx)))))
