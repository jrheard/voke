(ns voke.system.rendering
  (:require [plumbing.core :refer [safe-get-in]]
            [voke.pixi :refer [add-to-stage! remove-from-stage! entity->graphic make-renderer
                               make-stage render! update-obj-position!]]
            [voke.schemas :refer [Entity GameState System]])
  (:require-macros [schema.core :as sm]))

; TODO not yet implemented: removing entities (eg dead monsters, collided bullets, etc)
; TODO also what about when entities are no longer visible? should we remove their objects?
; watch perf first i guess and then see what happens

(defn handle-unknown-entities! [stage objects-by-entity-id entities]
  (doseq [entity entities]
    ; TODO - only actually operate on the entity if it's visible!!!!!!
    (let [obj (entity->graphic entity)]
      (add-to-stage! stage obj)
      (swap! objects-by-entity-id assoc (:id entity) obj))))

(defn handle-movement-event [stage objects-by-entity-id event]
  (if-let [obj (@objects-by-entity-id (-> event :entity :id))]
    (update-obj-position! obj (-> event :entity :position))
    (handle-unknown-entities! stage objects-by-entity-id [(event :entity)])))

(defn handle-remove-entity-event [stage objects-by-entity-id event]
  (remove-from-stage! stage
                      (@objects-by-entity-id (event :entity-id)))
  (swap! objects-by-entity-id dissoc (event :entity-id)))

(let [renderer (make-renderer 1000 700 (js/document.getElementById "screen"))
      stage (make-stage)
      objects-by-entity-id (atom {})]

  ;; Public

  (sm/defn render-tick
    [state :- GameState]
    (let [relevant-entities (filter #(and
                                      (contains? % :shape)
                                      (contains? % :renderable))
                                    (vals (state :entities)))
          unknown-entities (filter #(not (contains? @objects-by-entity-id
                                                    (:id %)))
                                   relevant-entities)]
      (handle-unknown-entities! stage objects-by-entity-id unknown-entities))
    (render! renderer stage))

  ;; System definition

  (sm/def render-system :- System
    {:event-handlers [{:event-type :movement
                       :fn         (partial handle-movement-event stage objects-by-entity-id)}
                      {:event-type :entity-removed
                       :fn         (partial handle-remove-entity-event stage objects-by-entity-id)}]}))
