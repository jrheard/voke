(ns voke.core
  (:require [cljs.spec.test :as stest]
            [voke.entity :as e]
            [voke.events]
            [voke.clock :refer [add-time!]]
            [voke.state :refer [make-game-state add-entity!]]
            [voke.system.core :refer [initialize-systems! process-a-tick]]
            [voke.system.rendering :refer [render-tick]]
            [voke.world.visualize :as visualize]))

(defonce player (e/player 500 300))

(defonce game-state
         (atom
           (make-game-state
             [player
              (e/wall 0 0 1000 30)
              (e/wall 0 30 30 640)
              (e/wall 970 29 30 642)
              (e/wall 0 670 1000 30)
              (e/monster 800 600)])))

; Useful in dev, so figwheel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(def timestep (/ 1000 60))
(defonce last-frame-time (atom nil))
(defonce time-accumulator (atom 0))

(defn -initialize! []
  (js/window.cancelAnimationFrame @animation-frame-request-id)
  (js/Collision.resetState)
  (voke.events/unsub-all!)
  (voke.state/flush! @game-state)
  (initialize-systems! @game-state (player :entity/id))
  (reset! last-frame-time (js/performance.now)))

(defn process-game-time [ts]
  (swap! time-accumulator + (min (- ts @last-frame-time) 200))
  (reset! last-frame-time ts)

  (while (>= @time-accumulator timestep)
    (add-time! timestep)
    (swap! game-state process-a-tick)
    (swap! game-state voke.state/flush!)

    (swap! time-accumulator - timestep))

  (render-tick @game-state))

(defn ^:export main []
  (-initialize!)

  (comment
    (stest/instrument `process-a-tick)
    (stest/instrument `voke.state/flush!))

  (js/window.requestAnimationFrame
    (fn handle-frame [ts]
      (if (= (@game-state :game-state/mode) :default)
        (do
          (process-game-time ts)
          (reset! animation-frame-request-id
                  (js/window.requestAnimationFrame handle-frame)))

        (visualize/main)))))

(comment
  (add-entity! (e/monster 800 600) :repl)

  (doseq [i (range 5)]
    (add-entity! (e/monster 800 (+ 200 (* i 100)))
                 :repl))
  )

