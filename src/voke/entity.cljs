(ns voke.entity
  (:require [cljs.spec :as s]
            [voke.specs]))

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
