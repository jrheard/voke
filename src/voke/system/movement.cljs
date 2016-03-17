(ns voke.system.movement
  (:require [clojure.set :refer [intersection difference]]
            [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
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

(def direction-value-mappings {:down  (/ Math/PI 2)
                               :up    (- (/ Math/PI 2))
                               :left  Math/PI
                               :right 0})

(def friction-value 0.80)
(def min-velocity 0.05)

(sm/defn remove-conflicting-directions :- [Direction]
  "Takes a seq of directions like #{:up :down :left} and returns a seq of directions
  where the pairs of conflicting directions - up+down, left+right - are stripped out if they're present."
  [directions :- [Direction]]
  (reduce (fn [directions conflicting-pair]
            (if (= (intersection directions conflicting-pair)
                   conflicting-pair)
              (difference directions conflicting-pair)
              directions))
          directions
          [#{:up :down} #{:left :right}]))

(sm/defn human-controlled-entity-movement-directions
  [entity :- Entity]
  (remove-conflicting-directions (safe-get-in entity [:input :intended-move-direction])))

(sm/defn should-update-orientation? :- s/Bool
  [entity :- Entity]
  (and
    ; TODO - support monsters
    (get-in entity [:input :intended-move-direction])
    (not-empty (human-controlled-entity-movement-directions entity))))

(sm/defn update-orientation :- Entity
  [entity :- Entity]
  ; TODO - currently only implemented for player-controlled entities, doesn't handle monsters
  (if (should-update-orientation? entity)
    (assoc-in entity
              [:shape :orientation]
              (let [directions (human-controlled-entity-movement-directions entity)
                    intended-direction-values (map direction-value-mappings directions)]
                (Math/atan2 (apply + (map Math/sin intended-direction-values))
                            (apply + (map Math/cos intended-direction-values)))))
    entity))

(sm/defn should-update-velocity? :- s/Bool
  [entity :- Entity]
  (or (should-update-orientation? entity)
      (and
        (safe-get-in entity [:motion :affected-by-friction])
        (or
          (not= (safe-get-in entity [:motion :velocity :x]) 0)
          (not= (safe-get-in entity [:motion :velocity :y]) 0)))))

(sm/defn get-acceleration :- s/Num
  [entity :- Entity]
  ; TODO - support monsters
  (if (not-empty (human-controlled-entity-movement-directions entity))
    (safe-get-in entity [:motion :max-acceleration])
    0))

(sm/defn ^:private -update-axis-velocity :- Entity
  [entity :- Entity
   axis :- Axis
   trig-fn :- (s/enum Math/cos Math/sin)]
  (let [axis-velocity (safe-get-in entity [:motion :velocity axis])
        new-velocity (+ axis-velocity
                        (* (get-acceleration entity)
                           (trig-fn (safe-get-in entity [:shape :orientation]))))
        max-speed (safe-get-in entity [:motion :max-speed])
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
  (if (safe-get-in entity [:motion :affected-by-friction])
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
    (seq (get-in entity [:input :intended-move-direction]))
    (not= (get-in entity [:motion :velocity :x] 0) 0)
    (not= (get-in entity [:motion :velocity :y] 0) 0)))

;; System definition

(sm/def move-system :- System
  {:tick-fn (fn move-system-tick [entities]
              (doseq [entity (filter relevant-to-movement-system? entities)]
                (let [moved-entity (-> entity
                                       update-orientation
                                       update-velocity
                                       apply-friction
                                       update-position)]

                  (when (not= moved-entity entity)
                    (attempt-to-move! entity
                                      (-> moved-entity :shape :center)
                                      (-> moved-entity :motion :velocity)
                                      entities)))))})
