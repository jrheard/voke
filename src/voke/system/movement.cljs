(ns voke.system.movement
  (:require [clojure.set :refer [intersection difference]]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Direction Entity GameState System]])
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

(sm/defn update-orientation :- Entity
  [entity :- Entity]
  ; TODO - currently only implemented for player-controlled entities, doesn't handle monsters/projectiles
  (assoc-in entity
            [:shape :orientation]
            (let [directions (remove-conflicting-directions (entity :intended-move-direction))
                  intended-direction-values (map direction-value-mappings directions)]
              (Math/atan2 (apply + (map Math/sin intended-direction-values))
                          (apply + (map Math/cos intended-direction-values))))))

(sm/defn update-velocity :- Entity
  [entity :- Entity]
  (let [acceleration (if (not-empty (remove-conflicting-directions (entity :intended-move-direction)))
                       (get-in entity [:motion :max-acceleration])
                       0)]
    (if (or (> acceleration 0)
            (> (or (get-in entity [:motion :velocity :x] 0)
                   (get-in entity [:motion :velocity :y] 0))))
      (let [orientation (get-in entity [:shape :orientation])
            update-axis-velocity (fn [trig-fn axis-velocity]
                                   (let [new-velocity (min (get-in entity [:motion :max-speed])
                                                        (* (+ axis-velocity
                                                              (* acceleration
                                                                 (trig-fn orientation)))
                                                           friction-value))]
                                     (if (> (Math/abs new-velocity) min-velocity)
                                       new-velocity
                                       0)))]
        (-> entity
            (update-in [:motion :velocity :x]
                       (partial update-axis-velocity Math/cos))
            (update-in [:motion :velocity :y]
                       (partial update-axis-velocity Math/sin))))
      entity)))

(sm/defn update-position :- Entity
  [entity :- Entity]
  (-> entity
      (update-in [:shape :x]
                 (fn [x]
                   (+ x (get-in entity [:motion :velocity :x]))))
      (update-in [:shape :y]
                 (fn [y]
                   (+ y (get-in entity [:motion :velocity :y]))))))

;; System definition

(sm/def move-system :- System
  {:every-tick {:fn (fn move-system-tick [entities publish-chan]
                      (doseq [entity (filter #(contains? % :intended-move-direction) entities)]
                        (let [moved-entity (-> entity
                                               update-orientation
                                               update-velocity
                                               update-position)]

                          (doseq [axis [:x :y]]
                            (publish-event publish-chan {:event-type   :intended-movement
                                                         :entity       entity
                                                         :axis         axis
                                                         :new-position (get-in moved-entity [:shape axis])
                                                         :new-velocity (get-in moved-entity [:motion :velocity axis])
                                                         :all-entities entities})))))}})
