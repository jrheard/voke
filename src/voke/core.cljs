(ns voke.core
  (:require [voke.entity :as e]
            [voke.schemas :refer [Entity GameState]]
            [voke.system.core :refer [make-system-runner]])
  (:require-macros [schema.core :as sm]))

(sm/defn make-game-state :- GameState
  [entities :- [Entity]]
  {:entities (into {}
                   (map (juxt :id identity)
                        entities))})

(defonce player (e/player 488 300))

; TODO :mode? :active-level?
(defonce game-state
         (atom
           (make-game-state
             [player
              (e/wall 0 0 1000 30)
              (e/wall 0 30 30 670)
              (e/wall 970 30 30 670)
              (e/wall 0 670 1000 30)])))

; Useful in dev, so figwheel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(defn ^:export main []
  (js/window.cancelAnimationFrame @animation-frame-request-id)

  ;(.profile js/console "hello")
  ;(js/window.setTimeout #(.profileEnd js/console "hello") 5000)

  (let [run-systems-fn (make-system-runner game-state (player :id))]

    (js/window.requestAnimationFrame (fn process-frame [ts]
                                       (swap! game-state run-systems-fn)
                                       (reset! animation-frame-request-id
                                               (js/window.requestAnimationFrame process-frame))))))

