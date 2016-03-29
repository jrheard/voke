(ns voke.core
  (:require [voke.entity :as e]
            [voke.events]
            [voke.clock :refer [add-time!]]
            [voke.state :refer [make-game-state]]
            [voke.system.core :refer [initialize-systems! process-a-tick]]
            [voke.system.rendering :refer [render-tick]]))

(defonce player (e/player 500 300))

; TODO :mode? :active-level?
(defonce game-state
         (atom
           (make-game-state
             [player
              (e/wall 500 15 1000 30)
              (e/wall 15 350 30 640)
              (e/wall 985 350 30 640)
              (e/wall 500 685 1000 30)
              (e/monster 800 600)])))

; Useful in dev, so figwheel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(def timestep (/ 1000 60))
(defonce last-frame-time (atom nil))
(defonce time-accumulator (atom 0))

(defn -initialize! []
  (js/window.cancelAnimationFrame @animation-frame-request-id)
  (voke.events/unsub-all!)
  (voke.state/flush!)
  (initialize-systems! @game-state (player :id))
  (reset! last-frame-time (js/performance.now)))

(defn ^:export main []
  (-initialize!)

  (js/window.requestAnimationFrame
    (fn process-frame [ts]
      (swap! time-accumulator + (min (- ts @last-frame-time) 200))
      (reset! last-frame-time ts)

      (while (>= @time-accumulator timestep)
        (add-time! timestep)
        (swap! game-state process-a-tick)
        (swap! game-state (voke.state/flush!))

        (swap! time-accumulator - timestep))

      (render-tick @game-state)

      (reset! animation-frame-request-id
              (js/window.requestAnimationFrame process-frame)))))

