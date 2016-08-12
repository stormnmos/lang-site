(ns lang-site.requests
  (:require
   [ajax.core :as ajax :refer [GET POST]]
   [datascript.core :as d]
   [lang-site.state :as state]
   [lang-site.components.templates :as templates]
   [secretary.core :as secretary :refer-macros [defroute]]))

(defn req
  [uri method handler]
  (ajax/ajax-request
   {:uri uri
    :method method
    :handler handler
    :format (ajax/transit-request-format)
    :response-format (ajax/transit-response-format)}))

(defmulti request
  (fn [uri handler]
    uri))

(defmethod request "/translation-group"
  [uri handler]
  (req uri :get handler))

(defmethod request "/api/schema"
  [uri handler]
  (req uri :get handler))

(defmethod request "/api/users"
  [uri handler]
  (req uri :get handler))
