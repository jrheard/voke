(ns voke.specs
  (:require [cljs.spec :as s]))

(s/def :entity/id int?)

(s/def :geometry/axis #{:geometry/x :geometry/y})
(s/def :geometry/x number?)
(s/def :geometry/y number?)
(s/def :geometry/vector2 (s/keys :req [:geometry/x :geometry/y]))
(s/def :geometry/direction (s/nilable number?))

;; Components

(s/def :shape/type #{:rectangle})
(s/def :shape/width number?)
(s/def :shape/height number?)
(s/def :shape/center :geometry/vector2)

(s/def :component/shape (s/keys :req [:shape/type :shape/center]
                                :opt [:shape/width :shape/height]))

(s/def :input/intended-direction #{:up :left :down :right})
(s/def :input/intended-move-direction (s/coll-of :input/intended-direction :kind set? :into #{}))
(s/def :input/intended-fire-direction (s/coll-of :input/intended-direction))

(s/def :component/input (s/keys :req [:input/intended-move-direction :input/intended-fire-direction]))

; When :motion/direction is nil, the entity isn't trying to move anywhere, so just let it sit or coast.
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

(s/def :weapon/last-attack-timestamp number?)
(s/def :weapon/fire-direction :geometry/direction)
(s/def :weapon/shots-per-second int?)
(s/def :weapon/shot-speed (s/and number? pos?))
(s/def :weapon/projectile-color number?)
(s/def :weapon/projectile-shape (s/keys :req [:shape/type]
                                        :opt [:shape/width :shape/height]))

(s/def :component/weapon (s/keys :req [:weapon/last-attack-timestamp
                                       :weapon/fire-direction
                                       :weapon/shots-per-second
                                       :weapon/shot-speed
                                       :weapon/projectile-color
                                       :weapon/projectile-shape]))

(s/def :collision/type #{:good-guy
                         :bad-guy
                         :obstacle
                         :trap
                         :projectile
                         :gold
                         :item
                         :explosion})
(s/def :collision/collides-with (s/coll-of :collision/type :kind set? :into #{}))
(s/def :collision/destroyed-on-contact boolean?)

(s/def :component/collision (s/keys :req [:collision/type]
                                    :opt [:collision/collides-with :collision/destroyed-on-contact]))

(s/def :render/fill number?)
(s/def :component/render (s/keys :req [:render/fill]))

(s/def :owned/owner-id :entity/id)
(s/def :component/owned (s/keys :req [:owned/owner-id]))

;; Entity

(s/def :entity/entity (s/keys :req [:entity/id]
                              :opt [:component/shape
                                    :component/motion
                                    :component/collision
                                    :component/render
                                    :component/owned
                                    :component/weapon
                                    :component/input]))

;; Game state

(s/def :game-state/entities (s/map-of :entity/id :entity/entity))
(s/def :game-state/game-state (s/keys :req [:game-state/entities]))
