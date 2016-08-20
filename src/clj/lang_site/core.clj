(ns lang-site.core
  (:require
   [compojure.core :refer [defroutes GET PUT POST ANY]]
   [compojure.route :as route]
   [compojure.handler :as handler]
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
                                               wrap-transit]]
   [com.stuartsierra.component :as component]))

(mount/start)

(defonce state (->> (state/new-state
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
        :body  (q/pull-translation-pair)})
  (GET "/translation-group/:squuid" [squuid]
       (pr-str (q/pull-translation-pair squuid)))
  (GET "/api/echo" request
       {:status 200
        :headers {"Content-Type" "application/transit"}
        :body request})
  (GET "/api/schema" []
       {:status 200
        :body (q/pull-schema)})
  (GET "/api/users" []
       {:status 200
        :body (q/pull-users)})
  (GET "/login" request "Login page.")
  (route/files "/" {:root "target"})
  (route/not-found "<h1>Page not found</h1>"))

(def handler
  (-> h/handler
      (transit/wrap-transit-response {:encoding :json})
      (wrap-keyword-params)
      (wrap-params)
      (wrap-session)))

;;; Main handler for transacting into datomic
(async/go
  (while true
    (let [unvalidated-tx (async/<! tx-chan)]
      (if-let [tx (db/validate-tx unvalidated-tx) ]
        (do
          (d/transact conn tx)
          (async/>! success-chan tx))
        (async/>! fail-chan unvalidated-tx)))))
