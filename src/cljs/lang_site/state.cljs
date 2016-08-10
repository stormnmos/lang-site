(ns lang-site.state
  (:require
   [cljs.core.async :as async :refer [<! >! put! take!]]))

(defonce events (async/chan 10))
(defonce transactions (async/chan 10))
