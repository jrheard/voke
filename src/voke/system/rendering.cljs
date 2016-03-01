(ns voke.system.rendering
  (:require [voke.pixi :refer [add-to-stage! entity->graphic make-renderer
                               make-stage render! update-obj-position!]]
            [voke.schemas :refer [Entity System]])
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

(sm/defn render-system-tick [renderer stage objects-by-entity-id entities publish-chan]
  (let [unknown-entities (filter #(not (contains? @objects-by-entity-id
                                                  (:id %)))
                                 entities)]
    (handle-unknown-entities! stage objects-by-entity-id unknown-entities))

  (render! renderer stage)
  entities)

(defn handle-movement-event [stage objects-by-entity-id event publish-chan]
  (if-let [obj (@objects-by-entity-id (-> event :entity :id))]
    (update-obj-position! obj (-> event :entity :shape))
    (handle-unknown-entities! stage objects-by-entity-id [(event :entity)])))

;; System definition

(sm/def render-system :- System
  (let [renderer (make-renderer 1000 700 (js/document.getElementById "screen"))
        stage (make-stage)
        objects-by-entity-id (atom {})]

    {:every-tick     {:reads #{:shape :renderable}
                      :fn    (fn [& args]
                               (apply render-system-tick renderer stage objects-by-entity-id args))}
     :event-handlers [{:event-type :movement
                       :fn         (partial handle-movement-event stage objects-by-entity-id)}]}))
