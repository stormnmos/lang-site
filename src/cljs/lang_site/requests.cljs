(ns lang-site.requests
  (:require
   [ajax.core :as ajax :refer [GET POST]]
   [datascript.core :as d]
   [lang-site.actions :as a]
   [lang-site.state :as state]
   [lang-site.components.templates :as templates]
   [secretary.core :as secretary :refer-macros [defroute]]))

(defn card-request-handler [[status body]]
  (.log js/console (str body))
  (d/transact! lang-site.core/conn [(templates/card body)]))

(defn schema-request-handler [response]
  (.log js/console (str response))
  (a/transact! state/events [response]))

(defn req
  [uri method handler]
  (ajax/ajax-request
   {:uri uri
    :method method
    :handler handler
    :format (ajax/transit-request-format)
    :response-format (ajax/transit-response-format)}))

(defmulti request
  (fn [uri]
    uri))

(defmethod request "/translation-group"
  [uri]
  (req uri :get card-request-handler))

(defmethod request "/api/schema"
  [uri]
  (req uri :get schema-request-handler))
