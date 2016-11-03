(ns voke.system.core
  (:require [cljs.spec :as s]
            [clojure.set :refer [difference]]
            [voke.events :refer [subscribe-to-event]]
            [voke.input :refer [handle-keyboard-events]]
            [voke.specs]
            [voke.system.ai.system :as ai]
            [voke.system.attack :as attack]
            [voke.system.camera :as camera]
            [voke.system.collision.system :as collision]
            [voke.system.damage :as damage]
            [voke.system.death :as death]
            [voke.system.health :as health]
            [voke.system.movement :as movement]
            [voke.system.rendering :as rendering]))

;; Private

(defn system-to-tick-fn
  "Takes a System map, returns a function of game-state -> game-state."
  [system]
  (fn [state]
    (let [updated-entities (into {}
                                 (map (juxt :entity/id identity)
                                      ((system :system/tick-fn) (vals (state :game-state/entities)))))]
      (assert (= (difference
                   (apply hash-set (map :entity/id updated-entities))
                   (apply hash-set (map :entity/id (state :game-state/entities))))
                 #{})
              "Tick functions shouldn't return new entities; use voke.state/add-entity! instead")

      (update-in state
                 [:game-state/entities]
                 merge
                 updated-entities))))

(s/fdef system-to-tick-fn
  :args (s/cat :system :system/system)
  :ret fn?)

; smell: collision system is listed first so that its tick function can reset its internal state atoms
; before anything else can happen in each frame.
; should systems have a :before-tick function that serves this purpose?
(def game-systems [collision/system
                   death/system
                   movement/system
                   attack/system
                   damage/system
                   health/system
                   ai/system
                   camera/system
                   rendering/system])

(def tick-functions
  (map system-to-tick-fn
       (filter :system/tick-fn game-systems)))

;; Public

(defn initialize-systems!
  [game-state player-entity-id]

  ; Run systems' initalize functions.
  (doseq [initialize-fn (keep identity
                              (map :system/initialize game-systems))]
    (initialize-fn game-state))

  ; Set up systems' event handlers.
  (doseq [event-handler-map (flatten
                              (keep identity
                                    (map :system/event-handlers game-systems)))]
    ; it's weird that this is :event/type, it should be :system/event-type, which maps to :voke.events/type
    (subscribe-to-event (event-handler-map :event/type)
                        (event-handler-map :system/event-handler-fn)))

  ; Listen to keyboard input.
  (handle-keyboard-events player-entity-id))

(defn process-a-tick
  "A function from game-state -> game-state, which you can call to make a unit
  of time pass in the game-world."
  [state]
  (reduce (fn [state tick-function]
            (tick-function state))
          state
          tick-functions))

(s/fdef process-a-tick
  :args (s/cat :state :game-state/game-state)
  :ret :game-state/game-state)
