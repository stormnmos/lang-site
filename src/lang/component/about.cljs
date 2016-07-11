(ns lang.components.about
  (:require [om.dom :as dom]))

(defn about []
  (dom/div #js {:className "about"}
           (dom/h2 nil "What is this?")
           (dom/p nil
                  "This is a test application")))
