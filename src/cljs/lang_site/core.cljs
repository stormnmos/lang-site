(ns lang-site.core
  (:require
   [mount.core :as mount]
   [lang-site.actions :as a]
   [lang-site.db :as db]
   [lang-site.components :as c]
   [lang-site.components.templates :as templates]
   [lang-site.db.mock-data :as m]
   [lang-site.requests :as req]
   [lang-site.state :as state :refer [conn events]]
   [lang-site.util :as u]
   [cognitect.transit :as t]
   [datascript.core :as d]
   [goog.dom :as gdom]
   [goog.events :as events]
   [om.core :as om :include-macros true]
   [cljs.core.async :as async :refer [<! >! chan put! take!
                                      poll! offer! mult tap]]
   [secretary.core :as secretary :refer-macros [defroute]]
   [cljs.pprint :as pprint])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:import goog.History))

(enable-console-print!)

(defroute api-schema "/api/schema" []
  (req/http-get "/api/schema" identity))

(defroute users "#/api/users" []
  (req/http-get "/api/users" templates/make-users))

(defroute register "/#api/register" []
  (req/http-get "/api/users" identity))

(defroute language-ids "/language-ids" []
  nil)

(defroute translation-group "#/translation-group" []
  (req/http-get "/translation-group" templates/card))

(defn run []
  (secretary/set-config! :prefix "#")
  (mount/start)
  (go
    (while true
      (let [tx (<! @events)]
        (try (d/transact! @conn tx)
             (catch js/Object e
               (.log js/console e))))))
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(om/root c/widget @conn
         {:react-key "root"
          :target (.getElementById js/document "lang-site")})

(defn on-js-reload []
  (om/root c/widget @conn
           {:react-key "root"
            :target (.getElementById js/document "lang-site")}))
