(ns lang-site.core
  (:require
   [ajax.core :as ajax :refer [GET POST]]
   [lang-site.actions :as a]
   [lang-site.db :as db]
   [lang-site.components :as c]
   [cognitect.transit :as t]
   [datascript.core :as d]
   [goog.dom :as gdom]
   [goog.events :as events]
   [om.core :as om :include-macros true]
   [sablono.core :as sab :refer-macros [html]]
   [cljs.core.async :as async :refer [<! >! put! take!]]
   [secretary.core :as secretary :refer-macros [defroute]]
   [cljs.pprint :as pprint])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:import goog.History))

(enable-console-print!)

(secretary/set-config! :prefix "#")

(defonce events (async/chan 10))
(defonce transactions (async/chan 10))

(defonce conn (db/create-db))
(defonce test-db (db/populate-db! conn))

(defn server-request-handler [status body]
  (.log js/console (str status body))
  (a/transact! events body))

(defn server-request
  ([uri]
   (server-request uri :get))
  ([uri method]
   (ajax/ajax-request
    {:uri uri
     :method method
     :handler server-request-handler
     :format (ajax/transit-request-format)
     :response-format (ajax/text-response-format)})))

(defroute users "/users/:eid" [eid]
  (a/transact! events {:db/eid eid :article/title "users"}))

(defroute article "/article/:eid" [eid]
  (a/transact! events {:db/id 0 :ui/article {:db/id (js/parseInt eid)}}))

(defroute location "/location/:eid" [eid]
  (a/transact! events {:db/id 0 :ui/article {:db/id (js/parseInt eid)}}))

(defroute categories "/category/:eid" [eid]
  (a/transact! events {:db/id 0 :ui/article {:db/id (js/parseInt eid)}}))

(defroute archive "/archive/:eid" [eid]
  (a/transact! events {:db/id 0 :ui/article {:db/id (js/parseInt eid)}}))

(defroute api-schema "/api/schema" []
  (server-request "/api/schema"))

(defroute language-ids "/language-ids" []
  nil)

(defroute translation-group "/translation-group" []
  (server-request "/translation-group"))

(defn run []
  (go
    (while true
      (d/transact! conn (<! events))
      (d/transact! conn (<! transactions))))
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true))
  (om/root c/widget conn
           {:shared {:events events}
            :target (. js/document (getElementById "app"))}))

(defonce app run)

(defn on-js-reload []
  (om/root c/widget conn
           {:shared {:events events}
            :target (. js/document (getElementById "app"))}))
