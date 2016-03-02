(ns voke.system.movement
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity GameState System]])
  (:require-macros [schema.core :as sm]))

(def direction-value-mappings {:down  (/ Math/PI 2)
                               :up    (- (/ Math/PI 2))
                               :left  Math/PI
                               :right 0})

; TODO everything below here is super fucking rough
(sm/defn update-orientation :- Entity
  [entity :- Entity]
  ; TODO - currently only implemented for player-controlled entities, doesn't handle monsters/projectiles
  (assoc-in entity
            [:shape :orientation]
            (let [intended-direction-values (map direction-value-mappings
                                                 (entity :intended-move-direction))]
              (Math/atan2 (apply + (map Math/sin intended-direction-values))
                          (apply + (map Math/cos intended-direction-values))))))

(sm/defn update-velocity :- Entity
  [entity :- Entity]
  (let [acceleration (if (not-empty (entity :intended-move-direction))
                       (get-in entity [:motion :max-acceleration])
                       0)
        orientation (get-in entity [:shape :orientation])
        update-axis-velocity (fn [trig-fn axis-value]
                               (min (get-in entity [:motion :max-speed])
                                    (* (+ axis-value
                                          (* acceleration
                                             (trig-fn orientation)))
                                       0.9)))]
    (-> entity
        (update-in [:motion :velocity :x]
                   (partial update-axis-velocity Math/cos))
        (update-in [:motion :velocity :y]
                   (partial update-axis-velocity Math/sin)))))

(sm/defn update-position :- Entity
  [entity :- Entity]
  (js/console.log (clj->js (get-in entity [:motion :velocity])))
  (-> entity
      (update-in [:shape :x]
                 (fn [x]
                   (+ x (get-in entity [:motion :velocity :x]))))
      (update-in [:shape :y]
                 (fn [y]
                   (+ y (get-in entity [:motion :velocity :y]))))))

;; System definition

; TODO if you collide with a wall you are stuck there forever
; upate the collision system to do something smarter to your velocity/position when you collide with something

(sm/def move-system :- System
  {:every-tick {:fn (fn move-system-tick [entities publish-chan]
                      (doseq [entity (filter #(contains? % :intended-move-direction) entities)]

                        ; TODO - problem!
                        ; if you're contacting a wall on the right, and you're trying to move up+right,
                        ; you *don't* move up because the position would be invalid!
                        ; you should be able to move up, though!
                        (publish-event publish-chan {:event-type   :intended-movement
                                                     :moved-entity (-> entity
                                                                       update-orientation
                                                                       update-velocity
                                                                       update-position)
                                                     :all-entities entities})))}})
