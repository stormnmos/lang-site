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
            [com.stuartsierra.component :as component]))

(defrecord State [url connection]
  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    (let [conn (d/connect url)]
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    (assoc component :connection nil)))

(defn new-state [url tx-chan fail-chan success-chan]
  (map->State {:url url
               :tx-chan tx-chan}))

(def state (->> (env :database-url)
                (new-state (async/chan 100)
                           (async/chan (async/sliding-buffer 10))
                           (async/chan (async/sliding-buffer 10)))
                       (component/start)))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes routes
  (GET "/translation-group" [] (q/pull-translation-pair (d/db conn)))
  (route/files "/" {:root "html"})
  (route/not-found "<h1>Page not found</h1>"))

(def handler
   routes)

;;; Main handler for transacting into datomic

(async/go
  (while true
    (let [{:keys [tx-chan fail-chan success-chan] state}
          unvalidated-tx (<! tx-chan)]
      (if-let [tx (validate-tx unvalidated-tx) ]
        (do (d/transact conn tx)
            (>! success-chan tx))
        (>! fail-chan unvalidated-tx)))))
