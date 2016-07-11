(ns cards.cards
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om.next :as omnext :refer-macros [defui]]
   [clojure.string :as string]
   [sablono.core :as sab :include-macros true]
   [devcards.core]
   [cljs.test :as t :include-macros true :refer-macros [testing is]])
  (:require-macros
   [devcards.core
    :as dc
    :refer [defcard defcard-doc deftest dom-node defcard-om-next]]))

(def ^:export front-matter
  {:layout false
   :title "The hard sell"
   :slug "devcards-the-hard-sell"
   :date "2015-06-06"
   :draft true
   :published false
   :base-card-options {:frame false}})

(enable-console-print!)

(defn calc-bmi [bmi-data]
  (let [{:keys [height weight bmi] :as data} bmi-data
        h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ weight (* h h)))
      (assoc data :weight (* bmi h h)))))

(defn slider [bmi-data param value min max]
  (sab/html
   [:input {:type "range" :value value :min min :max max
            :style {:width "100%"}
            :on-change (fn [e]
                         (swap! bmi-data assoc param (.-target.value e))
                         (when (not= param :bmi)
                           (swap! bmi-data assoc :bmi nil)))}]))

(defn bmi-component [bmi-data]
  (let [{:keys [weight height bmi]} (calc-bmi @bmi-data)
        [color diagnose] (cond
                           (< bmi 18.5) ["orange" "underweight"]
                           (< bmi 25)   ["inherit" "normal"]
                           (< bmi 30)   ["orange" "overweight"]
                           :else        ["red" "obese"])]
    (sab/html
     [:div
      [:h3 "BMI calculator"]
      [:div
       [:span (str "Height: " (int height) "cm")]
       (slider bmi-data :height height 100 200)]
      [:div
       [:span (str "weight: " (int weight) "kg")
        (slider bmi-data :weight weight 30 150)]
       [:div
        [:span (str "BMI: " (int bmi) " ")]
        [:span {:style {:color color}} diagnose]
        (slider bmi-data :bmi bmi 10 50)]]])))

(defcard bmi-calculator
  (fn [data-atom _] (bmi-component data-atom))
  {:height 180 :weight 80}
  {:inspect-data true
   :frame true
   :history true})

(defn om-slider [bmi-data param value min max]
  (sab/html
   [:input {:type "range" :value value :min min :max max
            :style {:width "100%"}
            :on-change (fn [e]
                         (om/update! bmi-data param (.-target.value e))
                         (when (not= param :bmi)
                           (om/update! bmi-data :bmi nil)))}]))

(defn om-bmi-component [bmi-data owner]
  (let [{:keys [weight height bmi]} (calc-bmi bmi-data)
        [color diagnose] (cond
                           (< bmi 18.5) ["orange" "underweight"]
                           (< bmi 25)   ["inherit" "normal"]
                           (< bmi 30)   ["orange" "overweight"]
                           :else        ["red" "obese"])]
    (om/component
     (sab/html
      [:div
       [:h3 "BMI calculator"]
       [:div
        [:span (str "Height: " (int height) "cm")]
        (om-slider bmi-data :height height 100 220)]
       [:div
        [:span (str "weight: " (int weight) "kg")]
        (om-slider bmi-data :weight weight 30 150)]
       [:div
        [:span (str "BMI: " (int bmi) " ")]
        [:span {:style {:color color}} diagnose]
        (om-slider bmi-data :bmi bmi 10 50)]]))))

(defcard om-support
  (dc/om-root om-bmi-component)
  {:height 100 :weight 80}
  {:inspect-data true
   :frame true
   :history true})

(defn bmi-mutate
  [{:keys [state]} _ params]
  (let [[k v] (first params)]
    {:action #(swap! state assoc k v)}))

(defn bmi-read
  [{:keys [state]} k {:keys [] :as params}]
  {:value (get @state k)})

(defn om-next-slider [c param value min max]
  (sab/html
   [:input {:type "range" :value value :min min :max max
            :style {:width "100%"}
            :on-change (fn [e]
                         (omnext/transact! c `[(change-bmi-key! {~param ~(.-target.value e)})])
                         (when (not= param :bmi)
                           (omnext/transact! c '[(change-bmi-key! {:bmi nil})])))}]))





(defui ^:once BmiComponent
  static omnext/IQuery
  (query [this]
    [:height :weight :bmi]) ; Pull these three values....
  Object ; Create the js object....
  (render [this]
    (let [props (omnext/props this)
          {:keys [weight height bmi]} (calc-bmi props)
          [color diagnose] (cond
                            (< bmi 18.5) ["orange" "underweight"]
                            (< bmi 25) ["inherit" "normal"]
                            (< bmi 30) ["orange" "overweight"]
                            :else ["red" "obese"])]
      (sab/html
       [:div
        [:h3 "BMI calculator"]
        [:div
         [:span (str "Height: " (int height) "cm")]
         (om-next-slider this :height height 100 220)]
        [:div
         [:span (str "Weight: " (int weight) "kg")]
         (om-next-slider this :weight weight 30 150)]
        [:div
         [:span (str "BMI: " (int bmi) " ")]
         [:span {:style {:color color}} diagnose]
         (om-next-slider this :bmi bmi 10 50)]]))))

(defonce bmi-reconciler
  (omnext/reconciler {:state {:height 180 :weight 80}
                      :parser (omnext/parser {:read bmi-read :mutate bmi-mutate})}))

(defcard
  "# Om Next support
   Here is the same calculator being rendered as an Om Next application.
   ```
   (defcard-om-next om-next-support
     BmiComponent
     bmi-reconciler
     {:inspect-data true :history true })
   ```
   ")

(defcard-om-next om-next-support
  BmiComponent
  bmi-reconciler
  {:inspect-data true
   :history true })

(defn translator-mutate
  [{:keys [state]} _ params]
  (let [[k v] (first params)]
    {:action #(swap! state assoc k v)}))

(defn translator-read
  [{:keys [state]} k {:keys [] :as params}]
  {:value (get @state k)})

(defui ^:once Translator
  static omnext/IQuery
  (query [this]
         [:question :answer])
  Object
  (render [this]
    (let [{:keys [question answer]} (omnext/props this)]
       (sab/html
        [:div
         [:h3 "Translate"
          [:div
           [:span question]]
          [:div.answer-template
           (map (fn [char] [:input {:max-length 1 :auto-complete "off"
                                    :type "text"}]) answer)]]]))))

(defonce translator-state
  {:verbs ["abandonar" "to abandon, leave behind, desert; to quit, give up"
           "abandono" "abandonas" "abandonamos" "abandon'ais" "abandonan"
           "abandonando"]})

(defonce translator-reconciler
  (omnext/reconciler {:state translator-state
                      :parser (omnext/parser {:read translator-read :mutate translator-mutate})}))

(defcard-om-next translator-test
  Translator
  translator-reconciler
  {:inspect-data true
   :history true})
