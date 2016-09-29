(ns voke.entity
  (:require [cljs.spec :as s]
            [voke.schemas :refer [Input Entity Weapon]])
  (:require-macros [schema.core :as sm]))

(s/def :geometry/x (s/and number? pos?))
(s/def :geometry/y (s/and number? pos?))
(s/def :geometry/vector2 (s/keys :req [:geometry/x :geometry/y]))
(s/def :geometry/direction (s/nilable number?))

;;; Specs

(s/def :entity/id int?)


; Component specs

(s/def :shape/type #{:rectangle})
(s/def :shape/width number?)
(s/def :shape/height number?)
(s/def :shape/center :geometry/vector2)

(s/def :component/shape (s/keys :req [:shape/type :shape/center]
                                :opt [:shape/width :shape/height]))

(s/def :input/intended-direction #{:up :left :down :right})
(s/def :input/intended-direction-set (s/coll-of :input/intended-direction :kind set?))
(s/def :input/intended-move-direction :input/intended-direction-set)
(s/def :input/intended-fire-direction :input/intended-direction-set)

(s/def :component/input (s/keys :req [:input/intended-move-direction :input/intended-fire-direction]))

(s/def :motion/direction :geometry/direction)
(s/def :motion/velocity :geometry/vector2)
(s/def :motion/affected-by-friction boolean?)
(s/def :motion/max-speed number?)
(s/def :motion/max-acceleration number?)

(s/def :component/motion (s/keys :req [:motion/velocity
                                       :motion/direction
                                       :motion/affected-by-friction
                                       :motion/max-speed
                                       :motion/max-acceleration]))

(s/def :weapon/last-attack-timestamp int?)
(s/def :weapon/fire-direction :geometry/direction)
(s/def :weapon/shots-per-second int?)
(s/def :weapon/projectile-shape (s/keys :req [:shape/type]
                                        :opt [:shape/width :shape/height]))

(s/def :component/weapon (s/keys :req [:weapon/last-attack-timestamp
                                       :weapon/fire-direction
                                       :weapon/shots-per-second
                                       :weapon/projectile-shape]))

(s/def :collision/type #{:good-guy
                         :bad-guy
                         :projectile
                         :obstacle
                         :trap
                         :projectile
                         :gold
                         :item
                         :explosion})
(s/def :collision/collides-with (s/coll-of :collision/type :kind set?))
(s/def :collision/destroyed-on-contact boolean?)

(s/def :component/collision (s/keys :req [:collision/type]
                                    :opt [:collision/collides-with :collision/destroyed-on-contact]))

(s/def :render/fill number?)
(s/def :component/render (s/keys :req [:render/fill]))

(s/def :owned/owner-id :entity/id)
(s/def :component/owned (s/keys :req [:owned/owner-id]))

(s/def :entity/entity (s/keys :req [:entity/id]
                              :opt [:component/shape
                                    :component/motion
                                    :component/collision
                                    :component/render
                                    :component/owned
                                    :component/weapon
                                    :component/input]))



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
    {:shape       {:width  25
                   :height 25
                   :type   :rectangle
                   :center {:x x :y y}}
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
    {:shape       {:width  25
                   :height 25
                   :type   :rectangle
                   :center {:x x :y y}}
     :motion      {:velocity             {:x 0 :y 0}
                   :affected-by-friction true
                   :direction            nil
                   :max-acceleration     1.5
                   :max-speed            4}
     :collision   {:type :bad-guy}
     :weapon      nil #_(make-weapon (- (/ Math/PI 2))
                                     0xFF0A00)
     ; xxx not schema'd / standardized yet, these are just some example values
     :ai          {:movement :basic
                   :attack   :basic}
     :render-info {:fill 0xB22822}}))

(sm/defn wall :- Entity
  [x y width height]
  (make-entity
    {:shape       {:width  width
                   :height height
                   :type   :rectangle
                   :center {:x x :y y}}
     :collision   {:type :obstacle}
     :render-info {:fill 0x333333}}))

(sm/defn projectile :- Entity
  [owner-id position collides-with projectile-shape projectile-color x-velocity y-velocity]
  (make-entity
    {:shape       (assoc projectile-shape
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
