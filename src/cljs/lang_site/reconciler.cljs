(ns lang-site.reconciler
  (:require [goog.object :as gobj]
            [om.next :as om]
            [lang.state :refer [initial-state]]))

(defmulti read om/dispatch)

(defmulti mutate om/dispatch)

(def parser
  (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler {:state initial-state
                  :parser parser}))
