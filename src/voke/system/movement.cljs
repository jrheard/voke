(ns voke.system.movement
  (:require [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Axis Direction Entity GameState System]]
            [voke.system.collision.system :refer [attempt-to-move!]]
            [voke.util :refer [bound-between]])
  (:require-macros [schema.core :as sm]))

; A dumb-as-rocks velocity/acceleration system.
; Based on http://www.randygaul.net/2012/02/22/basic-2d-vector-physics-acceleration-orientation-and-friction/ .
;
; If I were a better man, I would have implemented http://buildnewgames.com/gamephysics/
; or http://blog.wolfire.com/2009/07/linear-algebra-for-game-developers-part-2/
;
; This system was easy to implement and I don't need all the bells/whistles in the linear-algebra-based
; articles, though, so I don't feel *too* bad.

(def friction-value 0.80)
(def min-velocity 0.05)

(sm/defn should-update-velocity? :- s/Bool
  "Entities' velocity should be updated if they're intending to move somewhere
  or if they're currently moving and affected by friction."
  [entity :- Entity]
  (or (get-in entity [:motion :direction])
      (and
        (get-in entity [:motion :affected-by-friction])
        (or
          (not= (get-in entity [:motion :velocity :x]) 0)
          (not= (get-in entity [:motion :velocity :y]) 0)))))

(sm/defn get-acceleration :- s/Num
  [entity :- Entity]
  (if (get-in entity [:motion :direction])
    (get-in entity [:motion :max-acceleration])
    0))

(sm/defn -update-axis-velocity :- Entity
  [entity :- Entity
   axis :- Axis
   trig-fn :- (s/enum Math/cos Math/sin)]
  (let [axis-velocity (get-in entity [:motion :velocity axis])
        new-velocity (+ axis-velocity
                        (* (get-acceleration entity)
                           (trig-fn (get-in entity [:motion :direction]))))
        max-speed (get-in entity [:motion :max-speed])
        capped-velocity (bound-between new-velocity (- max-speed) max-speed)]
    (if (> (Math/abs capped-velocity) min-velocity)
      (assoc-in entity [:motion :velocity axis] capped-velocity)
      (assoc-in entity [:motion :velocity axis] 0))))

(sm/defn update-velocity :- Entity
  [entity :- Entity]
  (if (should-update-velocity? entity)
    (-> entity
        (-update-axis-velocity :x Math/cos)
        (-update-axis-velocity :y Math/sin))
    entity))

(sm/defn apply-friction :- Entity
  [entity :- Entity]
  (if (get-in entity [:motion :affected-by-friction])
    (reduce
      (fn [entity axis]
        (update-in entity [:motion :velocity axis] #(* % friction-value)))
      entity
      [:x :y])
    entity))

(sm/defn update-position :- Entity
  [entity :- Entity]
  (let [velocity (-> entity :motion :velocity)
        center (-> entity :shape :center)]
    (assoc-in entity
              [:shape :center]
              {:x (+ (velocity :x) (center :x))
               :y (+ (velocity :y) (center :y))})))

(sm/defn relevant-to-movement-system? :- s/Bool
  [entity :- Entity]
  (or
    (get-in entity [:motion :direction])
    (not= (get-in entity [:motion :velocity :x] 0) 0)
    (not= (get-in entity [:motion :velocity :y] 0) 0)))

;; System definition

(sm/def move-system :- System
  {:tick-fn (fn move-system-tick [entities]
              (doseq [entity (filter relevant-to-movement-system? entities)]
                (let [moved-entity (-> entity
                                       update-velocity
                                       apply-friction
                                       update-position)]

                  (when (not= moved-entity entity)
                    (attempt-to-move! entity
                                      (-> moved-entity :shape :center)
                                      (-> moved-entity :motion :velocity)
                                      entities)))))})
