(ns lang-site.actions
  (:require
   [cljs.core.async :as async :refer [<! >! put! take!]]
   [datascript.core :as d]
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

(defn add-card [eid db events]
  (req/request "/translation-group" card-request-handler)
  (transact! events [[:db/add 0 :app/grid-components (go (<! state/card-eid-queue))]
                     [:db/retract 0 :app/grid-components eid]]))
