(ns lang-site.state
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defrecord State [url tx-chan fail-chan
                  success-chan connection]
  component/Lifecycle
  (start [component]
    (println ";; Creating application state")
    (let [conn (d/connect url)]
      (assoc component :connection conn)))
  (stop [component]
    (println ";; Destroying application state")
    (assoc component :connection nil)))

(defn new-state [url tx-chan fail-chan success-chan]
  (map->State {:url url
               :tx-chan tx-chan
               :fail-chan fail-chan
               :success-chan success-chan}))
