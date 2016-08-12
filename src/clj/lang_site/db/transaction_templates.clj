(ns lang-site.db.transaction-templates
  (:require [datomic.api :as d]))

(defn user-template [[name email]]
  {:type :user
   :tx {:user/name name
        :user/email email}})

(defn links-template [eids]
  (let [squuid (d/squuid)]
    {:type :link
     :tx (mapv (fn [eid]
                 {:db/id eid
                  :sentence/group squuid})
               eids)}))

(defn sentence-template [[id lang text]]
  {:type :sentence
   :tx
   [{:db/id #db/id[:db.part/user] :sentence/id (read-string id)
     :sentence/language
     (cond
       (= "eng" lang) :sentence.language/eng
       (= "tur" lang) :sentence.language/tur)
     :sentence/text text}]})

(defn excise-template [tx-id]
  {:type :excise
   :tx [{:db/id #db/id[:db.part/user]
         :db/excise tx-id}]})
