(ns lang-site.core
  (:require
   [ajax.core :as ajax :refer [GET POST]]
   [lang-site.actions :as a]
   [lang-site.db :as db]
   [lang-site.components :as c]
   [lang-site.components.templates :as templates]
   [lang-site.requests :as req]
   [lang-site.state :as state]
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

(defonce conn (db/create-db))
(defonce test-db (db/populate-db! conn))

(defroute api-schema "/api/schema" []
  (req/request "/api/schema"))

(defroute users "#/api/users" []
  (req/request "/api/users"))

(defroute language-ids "/language-ids" []
  nil)

(defroute translation-group "#/translation-group" []
  (req/request "/translation-group"))

(defn run []
  (go
    (while true
      (let [tx (<! state/events)]
        (.log js/console (pr-str tx))
        (try (d/transact! conn tx)
             (catch js/Object e
               (.log js/console e))))))
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true))
  (om/root c/widget conn
           {:shared {:events state/events}
            :target (. js/document (getElementById "app"))}))

(defonce app run)

(defn on-js-reload []
  (om/root c/widget conn
           {:react-key "root"
            :shared {:events state/events}
            :target (. js/document (getElementById "app"))}))
