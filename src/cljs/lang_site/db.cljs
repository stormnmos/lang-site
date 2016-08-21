(ns lang-site.db
  (:require [datascript.core :as d]
            [datascript.db :as ddb]
            [lang-site.state :refer [conn]]))

(defn pea [att]
  (d/q '[:find (pull ?e [*])
         :in $ ?a
         :where [?e ?a]] (d/db @conn) att))

(defn pvea [eid att]
  (d/q '[:find (pull ?v [*])
         :in $ ?e ?a
         :where [?e ?a ?v]] (d/db @conn) eid att))

(defn vea [eid att]
  (d/q '[:find ?v .
         :in $ ?e ?a
         :where [?e ?a ?v]]
       (d/db @conn) eid att))

(defn eav [att v]
  (d/q '[:find ?e
         :in $ ?a ?v
         :where [?e ?a ?v]]
       (d/db @conn) att v))

(defn ea [att]
  (d/q '[:find ?e
         :in $ ?a
         :where [?e ?a]]
       (d/db @conn) att))

(defn p [dest pull-query eid]
  (dest (d/pull (d/db @conn) pull-query eid)))

(defn g [att eid]
  (att (d/pull (d/db @conn) [att] eid)))

(defn gets [att eid]
  ((first (keys att)) (d/pull (d/db @conn) [att] eid)))

(defn gv [atts eid]
  (map (fn [att] (g att eid)) atts))

(defn children
  ([eid]
   (map conj (eav :widget/owner eid) (repeat (d/db @conn))))
  ([vals eid]))

(defn ordered-children [eid]
  (apply map vector
         [(->> (d/pull (d/db @conn)
                       [{:widget/_owner [:db/id :widget/order]}] eid)
                :widget/_owner
                (sort-by :widget/order)
                (map :db/id))
          (repeat (d/db @conn))]))

(defn get-widgets [type]
  (d/q '[:find ?e
           :in $ ?v
           :where [?e :widget/type ?v]]
         (d/db @conn) type))

(defn get-widget [type]
  (d/q '[:find (min ?e) .
           :in $ ?v
           :where [?e :widget/type ?v]]
         (d/db @conn) type))

(defn get-ui-att [att]
  (g att 0))

(defn get-ui-comps [att]
  (mapv (fn [eid] (:db/id eid))
        (att (d/pull (d/db @conn) [{att [:db/id]}] 0))))

(defn set-att [eid att val]
  {:db/id eid
   att val})

(defn set-content [eid content]
  (set-att eid :widget/content content))

(defn get-att [att]
  (d/q '[:find ?v .
         :in $ ?a
         :where [_ ?a ?v]]
       (d/db @conn) att))

#_(defn cas [db e a ov nv]
  (let [e (ddb/entid-strict db e)
        _  (ddb/validate-attr db a)
        ov (if (ddb/ref? db a) (ddb/entid-strict db ov) ov)
        nv (if (ddb/ref? db a) (ddb/entid-strict db nv) nv)
        datoms ()]))
