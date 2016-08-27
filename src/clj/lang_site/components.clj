(ns lang-site.components)

(defmacro defwidget
  "Docstring for macro"
  [key & body]
  `(defmethod widgets ~key [eid# owner#]
     (reify
       Widget
       ~@body
       om/IInitState
       (~(symbol "init-state") [this#]
        {:listener (async/chan (async/dropping-buffer 1))})
       om/IRender
       (~(symbol "render") [this#]
        (sab/html
         (~(symbol "template") this# (d/touch (d/entity (d/db @~(symbol "conn")) eid#)))))
       om/IWillMount
       (~(symbol "will-mount") [this#]
        (let [listener# (om/get-state owner# :listener)]
          (d/listen! @~(symbol "conn") eid# #(~(symbol "offer!") listener# %))))
       om/IShouldUpdate
       (~(symbol "should-update") [this# _# _#]
        (when-let [tx-report# (async/poll! (om/get-state owner# :listener))]
          (not (== (d/touch (d/entity (:db-before tx-report#) eid#))
                   (d/touch (d/entity (d/db @~(symbol "conn")) eid#)))))))))
