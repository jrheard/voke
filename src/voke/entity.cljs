(ns voke.entity
  (:require [cljs.spec :as s]))

;; Specs

(s/def :entity/id int?)

(s/def :geometry/x number?)
(s/def :geometry/y number?)
(s/def :geometry/vector2 (s/keys :req [:geometry/x :geometry/y]))
(s/def :geometry/direction (s/nilable number?))

(s/def :shape/type #{:rectangle})
(s/def :shape/width number?)
(s/def :shape/height number?)
(s/def :shape/center :geometry/vector2)

(s/def :component/shape (s/keys :req [:shape/type :shape/center]
                                :opt [:shape/width :shape/height]))

(s/def :input/intended-direction #{:up :left :down :right})
(s/def :input/intended-move-direction (s/coll-of :input/intended-direction :kind set?))
(s/def :input/intended-fire-direction (s/coll-of :input/intended-direction))

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

;; Private functions

(defonce next-entity-id (atom 0))

(defn get-next-entity-id []
  (let [id-to-return @next-entity-id]
    (swap! next-entity-id inc)
    id-to-return))

(s/fdef get-next-entity-id
  :ret :entity/id)

(defn make-entity
  [entity-map]
  (assoc entity-map :entity/id (get-next-entity-id)))

(s/fdef make-entity
  :ret :entity/entity)

(defn make-input
  []
  #:input {:intended-move-direction #{}
           :intended-fire-direction []})

(s/fdef make-input
  :ret :component/input)

(defn make-weapon
  [fire-direction projectile-color]
  #:weapon {:last-attack-timestamp 0
            ; XXXX nuke fire-direction arg, just temporary thing for testing
            :fire-direction        fire-direction
            :shots-per-second      21
            :shot-speed            5
            :projectile-color      projectile-color
            :projectile-shape      #:shape {:type   :rectangle
                                            :width  10
                                            :height 10}})

(s/fdef make-weapon
  :ret :component/weapon)

;; Public

(defn player
  [x y]
  (make-entity
    #:component {:shape     {:shape/width  25
                             :shape/height 25
                             :shape/type   :rectangle
                             :shape/center {:geometry/x x
                                            :geometry/y y}}
                 :motion    {:motion/velocity             {:geometry/x 0
                                                           :geometry/y 0}
                             :motion/affected-by-friction true
                             :motion/direction            nil
                             :motion/max-acceleration     2.0
                             :motion/max-speed            11}
                 :collision {:collision/type :good-guy}
                 :render    {:render/fill 0x333333}
                 :weapon    (make-weapon nil 0x666666)
                 :input     (make-input)}))

(s/fdef player
  :args (s/cat :x int? :y int?)
  :ret :entity/entity)

(defn monster
  [x y]
  (make-entity
    #:component {:shape     {:shape/width  25
                             :shape/height 25
                             :shape/type   :rectangle
                             :shape/center {:geometry/x x
                                            :geometry/y y}}
                 :motion    {:motion/velocity             {:geometry/x 0
                                                           :geometry/y 0}
                             :motion/affected-by-friction true
                             :motion/direction            nil
                             :motion/max-acceleration     1.5
                             :motion/max-speed            4}
                 :collision {:collision/type :bad-guy}
                 ;:weapon      nil
                 #_(make-weapon (- (/ Math/PI 2))
                                0xFF0A00)
                 ; xxx not spec'd / standardized yet, these are just some example values
                 :ai        {:movement :basic
                             :attack   :basic}
                 :render    {:render/fill 0xB22822}}))

(s/fdef monster
  :ret :entity/entity)

(defn wall
  [x y width height]
  (make-entity
    #:component {:shape     {:shape/width  width
                             :shape/height height
                             :shape/type   :rectangle
                             :shape/center {:geometry/x x
                                            :geometry/y y}}
                 :collision {:collision/type :obstacle}
                 :render    {:render/fill 0x333333}}))

(s/fdef wall
  :ret :entity/entity)

(defn projectile
  [owner-id position collides-with projectile-shape projectile-color x-velocity y-velocity]
  (make-entity
    #:component {:shape     (assoc projectile-shape
                                   :shape/center position)
                 :owned     {:owner/id owner-id}
                 :collision {:collision/type                 :projectile
                             :collision/collides-with        collides-with
                             :collision/destroyed-on-contact true}
                 :render    {:render/fill projectile-color}
                 :motion    {:motion/velocity             {:geometry/x x-velocity
                                                           :geometry/y y-velocity}
                             :motion/direction            nil
                             :motion/affected-by-friction false
                             :motion/max-speed            (max x-velocity y-velocity)
                             :motion/max-acceleration     0}}))

(s/fdef projectile
  :ret :entity/entity)
