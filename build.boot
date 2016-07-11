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
 :source-paths #{"less" "src"}
 :resource-paths #{"html" "resources"}
 :dependencies '[; Boot Requirements
                 [adzerk/boot-cljs "1.7.228-1"]
                 [adzerk/boot-reload "0.4.11"]
                 [deraen/boot-less "0.5.0"]
                 [pandeiro/boot-http "0.7.3"]

                 ; Clojure and Clojurescript Dependencies
                 [org.clojure/clojure "1.9.0-alpha8"]
                 [org.clojure/clojurescript "1.9.93"]
                 [org.omcljs/om "1.0.0-alpha34"]

                 ; Devcards addon
                 [devcards "0.2.1-7"]

                 ;Client Side Application Requirements
                 [sablono "0.7.2"]
                 [enfocus  "2.1.1"]

                 ; Fix for boot-less
                 [org.slf4j/slf4j-nop "1.7.13" :scope "test"]

                 ; Datomic requirements
                 [com.datomic/datomic-pro "0.9.5372"]])
(task-options!
 pom {:project "lang-site"
      :version "0.1.0-SNAPSHOT"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-less :refer [less]]
         '[pandeiro.boot-http :refer [serve]])

(def +version+ "0.1.0")

(deftask run
  []
  (comp
   (watch)
   (speak)
   (reload)
   (less)
   (cljs :source-map true
         :optimizations :none
         :compiler-options {:devcards true})
   (serve :dir "target"
          :httpkit true)))

(deftask release
  []
  (comp
   (watch)
   (less :compression true)
   (cljs :optimizations :advanced
         :compiler-options {:devcards true})))

(deftask run-release
  []
  (comp
   (watch)
   (reload)
   (less :compression true)
   (cljs :optimizations :advanced
         :compiler-options {:devcards true})
   (serve :dir "target"
          :httpkit true)))
