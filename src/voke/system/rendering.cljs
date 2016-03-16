(ns voke.system.rendering
  (:require [voke.pixi :refer [render! update-entity-position! remove-entity!]]
            [voke.schemas :refer [Entity GameState System]])
  (:require-macros [schema.core :as sm]))

; TODO not yet implemented: removing entities (eg dead monsters, collided bullets, etc)
; TODO also what about when entities are no longer visible? should we remove their objects?
; watch perf first i guess and then see what happens

(defn handle-movement-event [event]
  (update-entity-position! (event :entity-id) (event :new-center)))

(defn handle-remove-entity-event [event]
  (remove-entity! (event :entity-id)))

;; Public

(sm/defn render-tick
  [state :- GameState]
  (let [relevant-entities (filter #(and
                                    (contains? % :shape)
                                    (contains? % :renderable))
                                  (vals (state :entities)))]
    (render! relevant-entities)))

;; System definition

(sm/def render-system :- System
  {:event-handlers [{:event-type :movement
                     :fn         handle-movement-event}
                    {:event-type :entity-removed
                     :fn         handle-remove-entity-event}]})
