(ns voke.system.collision.system
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Axis Entity EntityID System Vector2]]
            [voke.state :refer [remove-entity!]]
            [voke.system.collision.resolution :refer [resolve-collision]]
            [voke.system.collision.utils :refer [-track-entity -stop-tracking-entity
                                                 apply-movement find-contacting-entities]])
  (:require-macros [schema.core :as sm]))

; TODO - obstacles shouldn't be able to collide with each other
; will simplify world generation / wall placement

(sm/defn handle-contact
  [entity :- Entity
   contacted-entities :- [Entity]]
  (when (get-in entity [:collision :destroyed-on-contact])
    (remove-entity! (entity :id) :collision-system))

  (doseq [contacted-entity contacted-entities]
    (publish-event {:event-type :contact
                    :entities   [entity contacted-entity]})))

(defn attempt-to-move!
  [entity new-center new-velocity all-entities]
  (let [contacted-entities (find-contacting-entities entity new-center all-entities)]
    (if (> (count contacted-entities) 0)
      (do
        (handle-contact entity contacted-entities)
        (resolve-collision entity new-center new-velocity all-entities))
      (apply-movement entity new-center new-velocity))))

;; System definition

(sm/def collision-system :- System
  {:initialize     (fn [game-state]
                     (doseq [entity (vals (game-state :entities))]
                       (-track-entity entity)))

   :event-handlers [{:event-type :entity-added
                     :fn         (fn [event]
                                   (-track-entity (event :entity)))}
                    {:event-type :entity-removed
                     :fn         (fn [event]
                                   (-stop-tracking-entity (event :entity-id)))}]})
