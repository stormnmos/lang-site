(ns lang-site.handler
  (:require [bidi.ring :refer (make-handler)]
            [compojure.core :refer [defroutes GET PUT POST ANY]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [lang-site.db.queries :as q]
            [ring.util.response :as res]))

(defn index-handler
  [request]
  (res/response "Homepage"))

(defn article-handler
  [{:keys [route-params]}]
  (res/response (str "You are viewing article: " (:id route-params))))

(defn translation-group-handler [_]
  (res/response (q/pull-translation-pair)))

(defn echo-handler [request]
  (res/response request))

(defn users-handler [request]
  (res/response (q/pull-users)))

(defn login-handler [request]
  (res/response "Login page."))

(def handler
  (make-handler ["/" {"index.html" index-handler
                      ["articles/" :id "/article.html"] article-handler
                      "translation-group" translation-group-handler}]))

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
  (POST "/api/echo" request
        {:status 200
         :headers {"Content-Type" "application/transit"}
         :body request})
  (POST "/api/transact" request
        {:status 200
         :header {"Content-Type" "application/transit"}
         :body request})
  (POST "/api/users" [user email password]
        {:status 200
         :headers {"Content-Type" "applications/transit"}
         :body nil})
  (GET "/api/schema" []
       {:status 200
        :body (q/pull-schema)})
  (GET "/api/users" []
       {:status 200
        :body (q/pull-users)})
  (GET "/login" request "Login page.")
  (route/files "/" {:root "target"})
  (route/not-found "<h1>Page not found</h1>"))
