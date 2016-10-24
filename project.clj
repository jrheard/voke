(defproject voke "0.1.0-SNAPSHOT"
  :description "a roguelike"
  :url "http://jrheard.com/voke"
  :license {:name "MIT License"
            :url  "http://choosealicense.com/licenses/mit/"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha13"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.2.395"
                  :exclusions [org.clojure/tools.reader]]
                 [reagent "0.6.0"]
                 [cljsjs/pixi "3.0.10-0"]]

  :plugins [[lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "out"]

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src"]
                :figwheel     {:on-jsload "voke.world.visualize/main"}
                :compiler     {:preloads             [devtools.preload]
                               ;:main                 voke.core
                               :static-fns           true
                               :main                 voke.world.visualize
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/voke.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map           true
                               :source-map-timestamp true}}
               {:id           "min"
                :source-paths ["src"]
                :compiler     {:output-to     "resources/public/js/compiled/voke.js"
                               ; :main          voke.core
                               :main          voke.world.visualize
                               :optimizations :advanced
                               :output-dir    "resources/public/js/compiled/out-min"
                               :asset-path    "js/compiled/out-min"
                               :source-map    "resources/public/js/compiled/voke.js.map"
                               :externs       ["externs/collision.js"
                                               "externs/seedrandom.js"]
                               :pretty-print  false}}
               {:id           "test"
                :source-paths ["src" "test"]
                :compiler     {:output-to     "test_resources/test.js"
                               :main          voke.runner
                               :foreign-libs  [{:file     "resources/public/js/rbush.js"
                                                :provides ["rbush"]}
                                               {:file     "resources/public/js/collision.js"
                                                :provides ["js-collision"]}]
                               :optimizations :none}}]}

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.8.2"]
                                  [figwheel-sidecar "0.5.7"]
                                  [com.taoensso/tufte "1.0.2"]
                                  [org.clojure/test.check "0.9.0"]]
                   :plugins      [[lein-doo "0.1.7" :exclusions [[org.clojure/clojurescript]]]]}})
