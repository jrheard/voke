(ns voke.core
  (:require [voke.entity :as e]
            [voke.events]
            [voke.schemas :refer [Entity GameState]]
            [voke.system.core :refer [make-system-runner]])
  (:require-macros [schema.core :as sm]))

(sm/defn make-game-state :- GameState
  [entities :- [Entity]]
  {:entities (into {}
                   (map (juxt :id identity)
                        entities))})

(defonce player (e/player 500 300))

; TODO :mode? :active-level?
(defonce game-state
         (atom
           (make-game-state
             [player
              (e/wall 500 15 1000 30)
              (e/wall 15 350 30 640)
              (e/wall 985 350 30 640)

              (e/wall 500 685 1000 30)])))

; Useful in dev, so figwheel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(defn ^:export main []
  (js/window.cancelAnimationFrame @animation-frame-request-id)
  (voke.events/unsub-all!)

  (let [run-systems-fn (make-system-runner game-state (player :id))]

    (js/window.requestAnimationFrame (fn process-frame [ts]
                                       (swap! game-state run-systems-fn)
                                       (reset! animation-frame-request-id
                                               (js/window.requestAnimationFrame process-frame))))))

