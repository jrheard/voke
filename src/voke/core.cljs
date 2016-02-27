(ns voke.core
  (:require [voke.system.core :refer [handle-update-entity-events make-system-runner]]))

; TODO make-player function... somewhere.
(def player {:id                      1
             :position                {:x 488
                                       :y 300}
             :collision-box           {:width 25 :height 25}
             :render-info             {:shape :square}
             :human-controlled        true
             :intended-move-direction #{}
             :intended-fire-direction #{}})

; TODO :mode? :active-level?
(defonce game-state (atom {:entities {1 player}}))

; Useful in dev, so figwheel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(defn ^:export main []
  (js/window.cancelAnimationFrame @animation-frame-request-id)

  ;(.profile js/console "hello")
  ;(js/window.setTimeout #(.profileEnd js/console "hello") 5000)

  ; TODO actually get player's id
  (let [run-systems-fn (make-system-runner game-state (player :id))]

    (js/window.requestAnimationFrame (fn process-frame [ts]
                                       (swap! game-state run-systems-fn)
                                       (reset! animation-frame-request-id
                                               (js/window.requestAnimationFrame process-frame))))))

