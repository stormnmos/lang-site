(ns lang-site.util
  (:require [clojure.string :refer [join]]
            [datascript.core :as d]
            [datascript.transit :as dt]
            [om.core :as om]))

;; transit serialization

(defn db->string [db]
  (dt/write-transit-str db))

(defn string->db [s]
  (dt/read-transit-str s))

;; persisting DB between page reloads
(defn persist [db]
  (js/localStorage.setItem "lang-site/DB" (db->string db)))

(defn make [f eid]
  (om/build f eid {:react-key eid}))

(defn make-all [f eids]
  (map (partial make f) eids))
