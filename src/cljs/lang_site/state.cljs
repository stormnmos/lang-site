(ns lang-site.state
  (:require
   [cljs.core.async :as async :refer [<! >! chan put! take! mult]]
   [datascript.core :as d]
   [lang-site.db.mock-data :as m]
   [lang-site.util :as u])
  (:require-macros [mount.core :refer [defstate]]))

(defn create-db []
  (d/create-conn m/schema))

(defn populate-db! [conn]
  (if-let [stored (js/localStorage.getItem "lang-site/DB")]
    (let [stored-db (u/string->db stored)]
      (d/reset-conn! conn stored-db))
    (d/transact! conn m/fixtures)))

(defstate conn
  :start (let [conn (create-db)]
           (populate-db! conn)
           conn))

(defstate events
  :start (chan 10))

(defstate transactions
  :start (let [tx-chan (chan)
               tx-mult (mult tx-chan)]
           (d/listen! @conn #(put! tx-chan %))
           tx-mult))

(defstate card-queue
  :start (chan 100))

(defstate card-eid-queue
  :start (chan 110))
