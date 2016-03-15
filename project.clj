(defproject voke "0.1.0-SNAPSHOT"
  :description "a roguelike"
  :url "http://example.com/FIXME"
  :license {:name "DO WHAT THE FUCK YOU WANT TO BUT IT'S NOT MY FAULT PUBLIC LICENSE"
            :url "https://raw.githubusercontent.com/adversary-org/wtfnmf/c7b46d8114e3b3adcd9198e635b43f511c7c803d/COPYING.WTFNMFPL"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]
                 [figwheel-sidecar "0.5.0"]
                 [prismatic/plumbing "0.5.2"]
                 [prismatic/schema "1.0.4"]
                 [cljsjs/pixi "3.0.7-0"]]

  :plugins [[lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "voke.core/main"}
                :compiler {:main voke.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/voke.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/voke.js"
                           :main voke.core
                           :optimizations :advanced
                           :externs ["externs/collision.js"]
                           :pretty-print false}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
