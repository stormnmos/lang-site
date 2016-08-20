(ns lang-site.handler
  (:require [bidi.ring :refer (make-handler)]
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
                      "translation-group" translation-group-handler
                      true :not-found}]))
