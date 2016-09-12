(ns lang-site.util
  (:require [clojure.string :refer [join]]
            [cljs.spec :as s]
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

(extend-type datascript.impl.entity/Entity
  IMap
  (-dissoc [coll k]
    coll)
  ICollection
  (-conj [coll o]
    coll)
  IAssociative
  (-assoc [coll k v]
    coll))
