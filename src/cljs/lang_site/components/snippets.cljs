(ns lang-site.components.snippets
  :require [kioo.om :refer [content set-attr do-> substitute listen]]
  :require-macros [kioo.om :refer defsnippet deftemplate])

(deftemplate header "header.html"
  [{}])
