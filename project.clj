(defproject voke "0.1.0-SNAPSHOT"
  :description "a roguelike"
  :url "http://example.com/FIXME"
  :license {:name "DO WHAT THE FUCK YOU WANT TO BUT IT'S NOT MY FAULT PUBLIC LICENSE"
            :url  "https://raw.githubusercontent.com/adversary-org/wtfnmf/c7b46d8114e3b3adcd9198e635b43f511c7c803d/COPYING.WTFNMFPL"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"
                  :exclusions [org.clojure/tools.reader]]
                 [binaryage/devtools "0.8.2"]
                 [figwheel-sidecar "0.5.7"]
                 [prismatic/plumbing "0.5.3"]
                 [prismatic/schema "1.1.3"]
                 [cljsjs/pixi "3.0.10-0"]]

  :plugins [[lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-doo "0.1.7" :exclusions [[org.clojure/clojurescript]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "out"]

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src"]
                :figwheel     {:on-jsload "voke.core/main"}
                :compiler     {:main                 voke.core
                               :preloads             [devtools.preload]
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/voke.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map           true
                               :source-map-timestamp true}}
               {:id           "min"
                :source-paths ["src"]
                :compiler     {:output-to     "resources/public/js/compiled/voke.js"
                               :main          voke.core
                               :optimizations :advanced
                               :output-dir    "resources/public/js/compiled/out-min"
                               :asset-path    "js/compiled/out-min"
                               :source-map    "resources/public/js/compiled/voke.js.map"
                               :externs       ["externs/collision.js"]
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

  :figwheel {:css-dirs ["resources/public/css"]})
