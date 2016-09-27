(ns lang-site.db.transaction-templates
  (:require [datomic.api :as d]))

(defn tag-template [[eid value]]
  {:type :tag
   :tx {:db/id eid
        :sentence/tag value}})

(defn user-template [[name email]]
  {:type :user
   :tx {:db/id #db/id[:db.part/user]
        :user/name name
        :user/email email}})

(defn change-sentence-group [group eids]
  {:type :link
   :tx (mapv (fn [eid]
               {:db/id eid
                :sentence/group group})
             eids)})

(defn link [eid squuid]
  {:type :link
   :tx [{:db/id eid
         :sentence/group squuid}]})

(defn links-template [eids]
  (let [squuid (d/squuid)]
    {:type :link
     :tx (mapv (fn [eid]
                 {:db/id eid
                  :sentence/group squuid})
               eids)}))

(defn sentence [[id lang text]]
  {:type :sentence
   :tx
   [{:db/id #db/id[:db.part/user]
     :sentence/id (read-string id)
     :sentence/language (keyword (str "sentence.language/" lang))
     :sentence/text text}]})

(defn excise [tx-id]
  {:type :excise
   :tx [{:db/id #db/id[:db.part/user]
         :db/excise tx-id}]})
