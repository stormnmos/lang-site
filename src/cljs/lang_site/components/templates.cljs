(ns lang-site.components.templates)

(defn sentence [data]
  (merge {:widget/type :sentence} data))

(defn card [data]
  (.log js/console (str data))
  {:db/id -1
   :widget/type :card
   :card/title "New Card from Datomic"
   :card/sentences (mapv sentence data)
   :card/content "Placeholder content data"})
