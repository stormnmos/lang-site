(ns lang-site.state
  (:require
   [clojure.core.async :refer [chan sliding-buffer]]
   [environ.core :refer [env]]
   [mount.core :refer [defstate]]
   [datomic.api :as d]))

(defstate conn
  :start (d/connect (env :database-url)))

(defstate tx-chan
  :start (chan 10))

(defstate fail-chan
  :start (chan (sliding-buffer 10)))

(defstate success-chan
  :start (chan (sliding-buffer 10))  )
