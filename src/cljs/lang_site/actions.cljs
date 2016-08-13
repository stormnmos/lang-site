(ns lang-site.actions
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [datascript.db :as ddb]
   [om.core :as om]
   [lang-site.components.templates :as templates]
   [lang-site.db :as db]
   [lang-site.state :as state]
   [lang-site.requests :as req])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn transact! [events data]
  (go
    (>! events data)))

(defmulti transactions!
  (fn [transaction]
    (:type transaction)))

(defn card-request-handler [[status body]]
  (transact! state/events [(templates/card body)]))

(defn schema-request-handler [response]
  (d/transact! state/events [response]))

(defn users-request-handler [[status body]]
  (d/transact! state/events body))

(defn add-text [eid events owner order tag]
  (go (>! events [{:db/id -1 :widget/type :text
                   :widget/content "New"
                   :widget/owner owner
                   :widget/order order
                   :widget/tag tag}])))

(defn retract [_ eid events]
  (go (>! events [[:db.fn/retractEntity eid]])))

(defn not-active [owner]
  {:on-click  #(om/set-state! owner :show-dropdown true)
   :on-mouse-leave #(om/set-state! owner :show-dropdown false)})

(defn active [owner]
  {:on-mouse-enter #(om/set-state! owner :show-dropdown true)
   :on-mouse-leave #(om/set-state! owner :show-dropdown false)})

(defn validate-card [eid db]
  "confirm that eid should be deleted")

(defn next-card [eid db events]
  (async/take! state/card-queue identity)
  (go
    (transact! events [[:db.fn/cas 0 :app/grid-components
                         eid (async/<! state/card-eid-queue)]])))
