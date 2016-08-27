(ns lang-site.components
  (:require [om.core :as om :include-macros true]
            [datascript.core :as d]
            [lang-site.actions :as a]
            [lang-site.db :as db]
            [lang-site.util :as u]
            [lang-site.requests :as req]
            [lang-site.state :refer [conn events transactions]]
            [lang-site.components.snippets :as s]
            [lang-site.components.templates :as t]
            [sablono.core :as sab :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan put! take! tap offer!]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [lang-site.components :refer [defwidget]]))

(defprotocol Widget
  (children   [this])
  (remote     [this])
  (local-call [this])
  (template   [this data])
  (query      [this db]))

(defmulti widgets
  (fn [eid _]
    (db/g :widget/type eid)))

(defwidget :default s/default)
(defwidget :menu-item s/menu-item)
(defwidget :link s/link)
(defwidget :header s/header)
(defwidget :header-drawer s/header-drawer)
(defwidget :register-card s/register-card)
(defwidget :login-card s/login-card)
(defwidget :user-card s/user-card)
(defwidget :sentence s/sentence)
(defwidget :card s/card
  (remote
   [this]
   "/translation-group")
  om/IDidMount
  (did-mount
   [this]
   (if (> 10 (count (d/datoms (d/db @conn) :avet :widget/type :card)))
     (mapv #(req/http-get (remote this) t/card) (range 10)))))
(defwidget :grid s/grid)
(defwidget :page s/page)

(defn widget [_]
  (reify
    om/IRender
    (render [this]
      (let [header (db/get-widget :header)
            page   (db/get-widget :page)]
        (sab/html [:.app (u/make-all widgets [page])])))))


#_(defwidget :cloze-card
  (template
   [this {:keys [:db/id cloze-card/title :cloze-card/question]
          :or {db/id 0 cloze-card/title "Title not found" close-card/question 1}
          {answer-text :sentence/text :or {answer-text "Data not found"}}
          :cloze-card/answer}]
   [:.mdl-card.mdl-shadow--4dp.language-card
    [:.mdl-card__title-text
     [:h2.mdl-card__title-text title]]
    [:.mdl-card__supporting-text
     [:ul.mdl-list
      (u/make widgets (:db/id question))
      [:mdl-list__item.answer-item
       [:span.mdl-list__item-primary-content answer-text]]]]
    [:form {:action "#"}
     [:.mdl-textfield.mdl-js-textfield.mdl-textfield--floating-label
      [:input.mdl-textfield__input {:type "text" :id (str "translation" id)}]
      [:label.mdl-textfield__label {:for (str "translation" id)}
       "Translation"]]]
    [:.mdl-card__actions.mdl-card--border
     [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
      {:on-click #(a/next-card id)
       :disabled false}
      "Next Sentence"]]
    [:.mdl-card__menu
     [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
      [:i.material-icons "person"]]]]))
