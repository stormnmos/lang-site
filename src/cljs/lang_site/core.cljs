(ns lang-site.core
  (:require
   [lang-site.actions :as a]
   [lang-site.db :as db]
   [lang-site.components :as c]
   [datascript.core :as d]
   [goog.dom :as gdom]
   [goog.events :as events]
   [om.core :as om :include-macros true]
   [sablono.core :as sab :refer-macros [html]]
   [cljs.core.async :as async :refer [<! >! put! take!]]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:import goog.History))

(enable-console-print!)

(secretary/set-config! :prefix "#")

(defonce events (async/chan 10))

(defonce conn (db/create-db))
(defonce test-db (db/populate-db! conn))

(defn run []
  (go
    (while true
      (d/transact! conn (<! events))))
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true))
  (om/root c/widget conn
           {:shared {:events events}
            :target (. js/document (getElementById "app"))}))

run

(defn on-js-reload []
  (om/root c/widget conn
           {:shared {:events events}
            :target (. js/document (getElementById "app"))}))
