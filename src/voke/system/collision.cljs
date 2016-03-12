(ns voke.system.collision
  (:require [clojure.set :refer [difference]]
            [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Axis Entity EntityID Event Shape System Vector2]]
            [voke.state :refer [remove-entity! update-entity!]])
  (:require-macros [schema.core :as sm]))

; TODO - obstacles shouldn't be able to collide with each other
; will simplify world generation / wall placement

(sm/defn -track-entity
  [entity :- Entity]
  (js/Collision.addEntity (clj->js
                            (select-keys entity
                                         [:id :collision :shape]))))

(defn -update-entity-center
  [entity-id new-center]
  (js/Collision.updateEntity entity-id new-center))

(sm/defn -stop-tracking-entity
  [entity-id :- EntityID]
  (js/Collision.removeEntity entity-id))

(sm/defn find-entity-with-id :- Entity
  [entities :- [Entity]
   id :- EntityID]
  (some #(when (== (% :id) id) %)
        entities))

(sm/defn find-contacting-entity :- (s/maybe Entity)
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns another Entity if the space `entity` is trying to occupy is already filled,
  nil if the space `entity` is trying to occupy is empty."
  ; Critical path! Keep fast!
  [entity :- Entity
   new-center :- Vector2
   all-entities :- [Entity]]
  (let [{:keys [x y]} new-center]
    (when-let [contacting-entity-id (js/Collision.findContactingEntityID (entity :id)
                                                                         #js {:x x :y y})]
      (find-entity-with-id all-entities contacting-entity-id))))

(sm/defn find-closest-clear-spot :- (s/maybe s/Num)
  [entity :- Entity
   new-velocity :- Vector2
   contacted-entity :- Entity
   all-entities :- [Entity]]
  "Takes an :intended-movement event (for entity A) and the Entity that occupies the position that entity A
  is trying to move to (entity B). Finds the closest x- or y-value (depending on the value of `(event :axis)`)
  that entity A can occupy without contacting entity B and returns it if entity A fits there, or returns nil
  if no open spot exists."
  ; TODO - only supports rectangles
  ; XXXX TODO
  #_(let [shape1 (entity :shape)
        shape2 (contacted-entity :shape)
        arithmetic-fn (if (pos? new-velocity) - +)
        field (if (= axis :x) :width :height)
        axis-value-to-try (arithmetic-fn (get-in shape2 [:center axis])
                                         (/ (shape2 field) 2)
                                         (/ (shape1 field) 2)
                                         0.01)]
    (when-not (find-contacting-entity entity axis axis-value-to-try all-entities)
      axis-value-to-try))
  entity)

(sm/defn apply-movement
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2]
  "Fires events to notify the world that a particular entity should have a new position+velocity."
  (let [update-entity-fn (fn [entity]
                           (assert entity)
                           (-> entity
                               (assoc-in [:shape :center] new-center)
                               (assoc-in [:motion :velocity] new-velocity)))]
    (update-entity! (entity :id) :collision-system update-entity-fn)

    (-update-entity-center (entity :id) new-center)

    (publish-event {:event-type :movement
                    :entity     (update-entity-fn entity)})))

(sm/defn handle-contact
  [entity :- Entity
   new-velocity :- s/Num
   contacted-entity :- Entity
   all-entities :- [Entity]]
  (if (get-in entity [:collision :destroyed-on-contact])
    ; This entity should be destroyed on contact, and we're handling a contact. Destroy it!
    (remove-entity! (entity :id) :collision-system)

    ; This entity doesn't need to be destroyed on contact. Let it live.
    (if-let [closest-clear-spot (find-closest-clear-spot entity new-velocity contacted-entity all-entities)]
      ; Great, we found a clear spot nearby! Move there and stand still.
      (apply-movement entity closest-clear-spot {:x 0 :y 0})

      ; Couldn't find a clear spot; slow the entity down, it can try moving again next tick.
      (update-entity! (entity :id)
                      :collision-system
                      (fn [old-entity]
                        old-entity
                        ; XXXXXX TODO
                        #_(update-in old-entity
                                   [:motion :velocity axis]
                                   #(* % 0.7))))))

  (publish-event {:event-type :contact
                  :entities   [entity contacted-entity]}))

(defn attempt-to-move!
  [entity new-center new-velocity all-entities]
  (if-let [contacted-entity (find-contacting-entity entity new-center all-entities)]
    (handle-contact entity new-velocity contacted-entity all-entities)
    (apply-movement entity new-center new-velocity)))

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
