(ns voke.core
  (:require [cljs.spec.test :as stest]
            [voke.entity :as e]
            [voke.events]
            [voke.clock :refer [add-time!]]
            [voke.state :refer [make-game-state add-entity!]]
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
              ;(e/monster 800 600)
              ])))

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

(defn ^:export main []
  (-initialize!)

  (comment
    (stest/instrument `process-a-tick)
    (stest/instrument `voke.state/flush!))

  (js/window.requestAnimationFrame
    (fn process-frame [ts]
      (swap! time-accumulator + (min (- ts @last-frame-time) 200))
      (reset! last-frame-time ts)

      (while (>= @time-accumulator timestep)
        (add-time! timestep)
        (swap! game-state process-a-tick)
        (swap! game-state voke.state/flush!)

        (swap! time-accumulator - timestep))

      (render-tick @game-state)

      (reset! animation-frame-request-id
              (js/window.requestAnimationFrame process-frame)))))


(comment
  ; lightning talk outline

  ; show game
  ; move around
  ; run into walls, bitch about collision system being hard
  ; shoot while stationary
  ; shoot while moving around

  ; spawn stationary monster, run into it, mention collisions
  ; have monster fire poorly
  ; tweak monster firing code until it tracks the player
  ; have the monster start following the player
  ; spawn five monsters, kill them all

  ; explain that what just happened was: i wrote some code and hit save,
  ; and my program immediately updated
  ; this happened like 5 or 10 times while we were talking just now, instantly

  ; back in the day (before my time) you had to write programs on punch cards and send them
  ; down the hall to the people who ran the computer, and would get results back days or weeks later

  ; we've improved a lot since then, but even so, the standard model of programming these
  ; days looks like: write code, start program from scratch, wait for it to run, kill program, repeat
  ; and during those five or ten seconds, you often get distracted and check your email
  ; and suddenly it's ten minutes later and you've forgotten what you were doing

  ; but this game is written in a language called clojurescript, and programmers who use clojurescript
  ; are able to see what their code does instantaneously, without having to wait ten seconds
  ; for their program to restart

  ; this instant reloading might seem like it's not a big deal, but it really gives you
  ; an incredible ability to just try things out, make changes, iterate, and stay focused
  ; the whole time

  ; plus it's frankly just like really really fun, like _really_ fun

  ; clojurescript isn't the first or only language to enable this kind of workflow, but
  ; it's really a pretty rare thing (programmers using python, c, java, etc don't program this way)

  ; anyway the game still obviously has a long way to go, but that's what it looks like so far

  (add-entity! (e/monster 800 600) :repl)


  (doseq [i (range 5)]
    (add-entity! (e/monster 800 (+ 200 (* i 100)))
                 :repl))
  )
