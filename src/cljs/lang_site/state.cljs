(ns lang-site.state
  (:require
   [cljs.core.async :as async :refer [<! >! put! take!]]
   [datascript.core :as d]
   [lang-site.db.mock-data :as m])
  (:require-macros [mount.core :refer [defstate]]))

(defn create-db []
  (d/create-conn m/schema))

(defn populate-db! [conn]
  (d/transact! conn m/fixtures))

(defstate conn
  :start (let [conn (create-db)]
           (populate-db! conn)
           conn))

(defstate events
  :start (async/chan 10))

(defstate transactions
  :start (async/chan 10))

(defstate card-queue
  :start (async/chan 100))

(defstate card-eid-queue
  :start (async/chan 110))
