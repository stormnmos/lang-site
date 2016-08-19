#!/usr/bin/env boot

(import java.io.File)

(set-env!
 :repositories
 #(conj %
        ["datomic"
         (merge {:url "https://my.datomic.com/repo"}
                (:datomic (gpg-decrypt
                           (File. (boot.App/bootdir) "credentials.edn.gpg"))))]))

(set-env!
 :source-paths #{"less" "src/clj" "src/cljc" "src/cljs"}
 :resource-paths #{"html" "resources" "templates"}
 :dependencies '[;; Boot Requirements
                 [adzerk/boot-cljs "1.7.228-1"]
                 [adzerk/boot-reload "0.4.11"]
                 [deraen/boot-less "0.5.0"]
                 [pandeiro/boot-http "0.7.3"]
                 ;; Environ Requirements
                 [environ "1.0.3"]
                 [boot-environ "1.0.3"]
                 ;;  Clojure and Clojurescript Dependencies
                 [org.clojure/clojure "1.9.0-alpha8"]
                 [org.clojure/clojurescript "1.9.93"]
                 [org.omcljs/om "1.0.0-alpha34"]
                 ;; Devcards addon
                 [devcards "0.2.1-7"]
                 ;; Clojurescript REPL
                 [adzerk/boot-cljs-repl   "0.3.2"]
                 [com.cemerick/piggieback "0.2.1"  :scope "test"]
                 [weasel                  "0.7.0"  :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 ;; Client Side Application Requirements
                 [sablono "0.7.2"]         ;;; client side html rendering
                 [datascript "0.15.2"]     ;;; client side db holding app
                 [secretary "1.2.3"]
                 [cljs-http "0.1.41"]
                 [cljs-ajax "0.5.8"]
                 [om-sync "0.1.1"]
                 [ring-transit "0.1.6"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cemerick/friend "0.2.3"]
                 ;; Server Side Requirements
                 [ring "1.5.0"]
                 [compojure "1.5.1"]
                 [com.stuartsierra/component "0.3.1"]
                 ;; Client and Server
                 [datascript-transit "0.2.1"]
                 ;; Fix for boot-less
                 [org.slf4j/slf4j-nop "1.7.13" :scope "test"]
                 ;; Datomic requirements
                 [com.datomic/datomic-pro "0.9.5372"]
                 [com.couchbase.client/couchbase-client "1.3.2"]
                 [io.netty/netty "3.6.3.Final"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-less :refer [less]]
         '[pandeiro.boot-http :refer [serve]]
         '[environ.boot :refer [environ]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])

(task-options!
 pom {:project "lang-site"
      :version "0.1.1"}
 environ {:env (merge {:database-url
                       "datomic:couchbase://localhost:4334/datomic/lang-site/?password=password"
                       :schema-file "resources/data/lang-site-schema.edn"
                       :sentence-file "resources/data/sentences.csv"
                       :links-file "resources/data/links.csv"}
                      (gpg-decrypt
                       (File. (boot.App/bootdir) "credentials.edn.gpg")))}
 reload {:on-jsload 'lang-site.core/on-js-reload}
 serve {:dir "target"
        :httpkit true
        :handler 'lang-site.core/handler
        :reload true})

(def +version+ "0.1.1")

(deftask run
  []
  (comp
   (environ)
   (watch)
   (speak)
   (reload)
   (less)
   (cljs-repl)
   (cljs :source-map true
         :optimizations :none
         :compiler-options {:devcards true})
   (target :dir #{"target"})
   (serve
    :dir "target"
    :httpkit true
    :handler 'lang-site.core/handler
    :reload true)))

(deftask run-prod
  []
  (comp
   (environ)
   (watch)
   (speak)
   (reload)
   (less)
   (cljs-repl)
   (cljs :source-map true
         :optimizations :advanced)
   (target :dir #{"target"})
   (serve)))

(deftask release
  []
  (comp
   (watch)
   (environ)
   (less :compression true)
   (cljs :optimizations :advanced
         :compiler-options {:devcards true})))

(deftask run-release
  []
  (comp
   (watch)
   (reload)
   (environ)
   (less :compression true)
   (cljs :optimizations :advanced
         :compiler-options {:devcards true})
   (serve :dir "target"
          :httpkit true)))
