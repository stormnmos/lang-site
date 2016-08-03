(ns lang-site.util
  (:require [clojure.string :refer [join]]
            [om.core :as om]))

(defn make [f eid]
  (om/build f eid {:react-key (first eid)}))

(defn make-all [f eids]
  (map (partial make f) eids))
