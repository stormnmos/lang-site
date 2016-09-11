(ns lang-site.state
  (:require
   [cljs.core.async :as async :refer [<! >! chan put! take! mult]]
   [cljs.spec :as s]
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

(s/def :widget/type keyword?)
(s/def :db/id int?)
(s/def ::widget (s/keys :req [:db/id :widget/type]))
(s/def :card/content (s/and (s/+ int?) #(d/entity (d/db conn) %)))
(s/def :card/title string?)
(s/def :card/question int?)
(s/def :card/answer int?)
(s/def ::card (s/keys :req [:db/id :widget/type :card/title
                            :card/question :card/answer
                            :card/content]))
(s/def :cloze-card/title string?)
(s/def :cloze-card/answer int?)
(s/def :cloze-card/question int?)
(s/def ::cloze-card
  (s/keys :req [:db/id :widget/type
                :cloze-card/answer
                :cloze-card/title
                :cloze-card/question]))
