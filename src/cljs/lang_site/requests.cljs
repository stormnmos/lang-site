(ns lang-site.requests
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :as async :refer [<! >!]]
   [lang-site.state :refer [events]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defn http-get [uri template]
  (async/pipe
   (http/get uri {:channel (async/chan 1 (comp (map :body) (map template)))})
   @events
   false))

(defn http-post [uri param-map]
  (http/post uri {:transit-params param-map}))
