(ns lang-site.core
  (:require [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [datomic.api :as d]
            [environ.core :refer [env]]
            [lang-site.db.db :as db]
            [lang-site.db.queries :as q]))

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
