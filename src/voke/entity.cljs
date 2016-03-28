(ns voke.entity
  (:require [voke.schemas :refer [Input Entity Weapon]])
  (:require-macros [schema.core :as sm]))

(defonce next-entity-id (atom 0))

(defn get-next-entity-id []
  (let [id-to-return @next-entity-id]
    (swap! next-entity-id inc)
    id-to-return))

(sm/defn make-entity :- Entity
  [entity-map]
  (assoc entity-map :id (get-next-entity-id)))

(sm/defn make-input :- Input
  []
  {:intended-move-direction #{}
   :intended-fire-direction []})

(sm/defn make-weapon :- Weapon
  [fire-direction projectile-color]
  {:last-attack-timestamp 0
   ; XXXX nuke fire-direction arg, just temporary thing for testing
   :fire-direction        fire-direction
   :shots-per-second      21
   :shot-speed            5
   :projectile-color      projectile-color
   :projectile-shape      {:type   :rectangle
                           :width  10
                           :height 10}})

;; Public

(sm/defn player :- Entity
  [x y]
  (make-entity
    {:shape       {:width       25
                   :height      25
                   :type        :rectangle
                   :orientation 0
                   :center      {:x x :y y}}
     :motion      {:velocity             {:x 0 :y 0}
                   :affected-by-friction true
                   :direction            nil
                   :max-acceleration     2.0
                   :max-speed            11}
     :collision   {:type :good-guy}
     :render-info {:fill 0x333333}
     :weapon      (make-weapon nil 0x666666)
     :input       (make-input)}))

(sm/defn monster :- Entity
  [x y]
  (make-entity
    {:shape       {:width       25
                   :height      25
                   :type        :rectangle
                   :orientation 0
                   :center      {:x x :y y}}
     :motion      {:velocity             {:x 0 :y 0}
                   :affected-by-friction true
                   :direction            nil
                   :max-acceleration     1.5
                   :max-speed            8}
     :collision   {:type :bad-guy}
     :weapon      (make-weapon (- (/ Math/PI 2))
                               0xFF0A00)
     :render-info {:fill 0xB22822}}))

(sm/defn wall :- Entity
  [x y width height]
  (make-entity
    {:shape       {:width       width
                   :height      height
                   :type        :rectangle
                   :orientation 0
                   :center      {:x x :y y}}
     :collision   {:type :obstacle}
     :render-info {:fill 0x333333}}))

(sm/defn projectile :- Entity
  [owner-id position collides-with projectile-shape projectile-color orientation x-velocity y-velocity]
  (make-entity
    {:shape       (assoc projectile-shape
                         :orientation orientation
                         :center position)
     :owner-id    owner-id
     :collision   {:type                 :projectile
                   :collides-with        collides-with
                   :destroyed-on-contact true}
     :render-info {:fill projectile-color}
     :motion      {:velocity             {:x x-velocity
                                          :y y-velocity}
                   :direction            nil
                   :affected-by-friction false
                   :max-speed            (max x-velocity y-velocity)
                   :max-acceleration     0}}))
