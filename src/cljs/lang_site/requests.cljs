(ns lang-site.requests
  (:require
   [ajax.core :as ajax :refer [GET POST]]
   [cljs-http.client :as http]
   [cljs.core.async :as async :refer [<! >!]]
   [datascript.core :as d]
   [lang-site.state :refer [events]]
   [lang-site.components.templates :as templates]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defn http-get [uri template]
  (async/pipe
   (http/get uri {:channel (async/chan 1 (comp (map :body)
                                               (map template)))})
   @events
   false))

(defn http-post [uri param-map]
  (.log js/console (str param-map))
  (async/pipe
   (http/post uri {:transit-params param-map
                   :channel (async/chan 1 (comp (map :body)
                                                #_(map :transit-params)))})
   @events
   false))
