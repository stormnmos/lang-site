(ns lang-site.core
  (:require
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :refer [make-auth]]
   [compojure.core :refer [defroutes GET PUT POST]]
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
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.transit :as transit :only [wrap-transit-body
                                               wrap-transit]]
   [com.stuartsierra.component :as component]))



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
  (GET "/api/users" []
       {:status 200
        :body (q/pull-users state)})
  (GET "/admin" request (friend/authorize #{::admin}
                                          "Admin Page."))
  (GET "/login" request "Login page.")
  (route/files "/" {:root "target"})
  (route/not-found "<h1>Page not found</h1>")
  (friend/logout (POST "/logout" [] "logging out")))

#_(def handler
  (transit/wrap-transit-response routes {:encoding :json}))

(def handler
  (-> routes
      (transit/wrap-transit-response {:encoding :json})
      (wrap-keyword-params)
      (wrap-params)
      (wrap-session)))

;;; Main handler for transacting into datomic
(async/go
  (while true
    (let [{:keys [tx-chan fail-chan success-chan]} state
          unvalidated-tx (async/<! tx-chan)]
      (if-let [tx (db/validate-tx unvalidated-tx) ]
        (do (d/transact (:connection state) tx)
            (async/>! success-chan tx))
        (async/>! fail-chan unvalidated-tx)))))
