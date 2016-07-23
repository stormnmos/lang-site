(ns lang-site.core
  (:require [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [datomic.api :as d]
            [environ.core :refer [env]]
            [lang-site.db.db :as db]
            [lang-site.db.queries :as q]
            [com.stuartsierra.component :as component]))

(defrecord Database [url connection]
  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    (let [conn (d/connect url)]
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    (assoc component :connection nil)))

(defn new-database [url]
  (map->Database {:url url}))

(def comp (->> (env :database-url)
                       (new-database)
                       (component/start)))

(def conn (d/connect (env :database-url)))

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
