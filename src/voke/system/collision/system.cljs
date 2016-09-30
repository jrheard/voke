(ns voke.system.collision.system
  (:require [cljs.spec :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Axis Entity EntityID System Vector2]]
            [voke.system.collision.resolution :refer [resolve-collision]]
            [voke.system.collision.state :refer [contacts-fired dead-entities]]
            [voke.system.collision.util :refer [-track-entity -stop-tracking-entity
                                                apply-movement find-contacting-entities]])
  (:require-macros [schema.core :as sm]))

; TODO - obstacles shouldn't be able to collide with each other
; will simplify world generation / wall placement

(defn handle-contact
  [entity contacted-entities]
  (doseq [contacted-entity contacted-entities]
    (let [id-pair #{(entity :entity/id) (contacted-entity :entity/id)}]
      ; Use the contacts-fired atom to dedupe contact events, so we don't fire multiple events
      ; for the same id-pair in the same frame.
      (when-not (contains? @contacts-fired id-pair)
        (swap! contacts-fired conj id-pair)
        (publish-event {:event/type :contact
                        :entities   [entity contacted-entity]})))))

(s/fdef handle-contact
  :args (s/cat :entity :entity/entity
               :contacted-entities (s/coll-of :entity/entity)))

(defn attempt-to-move!
  [entity new-center new-velocity all-entities]
  (when-not (contains? @dead-entities (entity :entity/id))
    (let [contacted-entities (find-contacting-entities entity new-center all-entities)]
      (if (seq contacted-entities)
        (do
          (handle-contact entity contacted-entities)
          (resolve-collision entity new-center new-velocity contacted-entities all-entities))
        (apply-movement entity new-center new-velocity)))))

;; System definition

(def collision-system
  {:system/initialize     (fn [game-state]
                            (doseq [entity (vals (game-state :game-state/entities))]
                              (-track-entity entity)))

   :system/tick-fn        (fn [_]
                            (reset! dead-entities #{})
                            (reset! contacts-fired #{}))

   :system/event-handlers [{:event/type              :entity-added
                            :system/event-handler-fn (fn [event]
                                                       (-track-entity (event :entity)))}
                           {:event/type              :entity-removed
                            :system/event-handler-fn (fn [event]
                                                       (-stop-tracking-entity (event :entity-id)))}]})
