(ns voke.system.collision
  (:require [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity Event]])
  (:require-macros [schema.core :as sm]))

(sm/defn shapes-collide? :- s/Bool
  [shape1 shape2]
  ; right now everything's just aabbs
  ; when that changes, this function will need to get smarter
  ; TODO also will need to rework this when x/y positions become center of rectangles; add helper functions
  ; or just a let statement with like left-x right-x etc
  (not-any? identity
            [(< (+ (shape1 :y) (shape1 :height))
                (shape2 :y))
             (> (shape1 :y)
                (+ (shape2 :y) (shape2 :height)))
             (> (shape1 :x)
                (+ (shape2 :x) (shape2 :width)))
             (< (+ (shape1 :x) (shape1 :width))
                (shape2 :x))]))

(sm/defn find-contacting-entity :- (s/maybe Entity)
  "TODO: DOCUMENT SEMANTICS"
  [entity :- Entity
   all-entities :- [Entity]]
  (let [entity-shape (entity :shape)
        collidable-entities (filter (fn [another-entity]
                                      (and
                                        (contains? entity :collision)
                                        (not= (entity :id) (another-entity :id))))
                                    all-entities)]
    (first (filter #(shapes-collide? (% :shape)
                                     entity-shape)
                   collidable-entities))))

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (let [entity (event :moved-entity)]
    (if-let [contacted-entity (find-contacting-entity entity (event :all-entities))]
      ; New position wasn't clear; contact!
      (publish-event publish-chan {:event-type :contact
                                   :entities   [entity contacted-entity]})

      ; Position was clear, go ahead and apply the intended movement.
      (do
        (publish-event publish-chan {:event-type :update-entity
                                     :origin     :collision-system
                                     :entity-id  (entity :id)
                                     :args       [[:shape] merge (entity :shape)]})
        (publish-event publish-chan {:event-type :movement
                                     :entity     entity})))))

(def collision-system
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
