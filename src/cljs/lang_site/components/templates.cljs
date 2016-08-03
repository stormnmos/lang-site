(ns lang-site.components.templates
  (:require
   [lang-site.util :as u]
   [kioo.om :refer [content set-attr do-> substitute listen]]
   [kioo.core :refer [handle-wrapper]])
  (:require-macros [kioo.om :refer [defsnippet deftemplate]]))
