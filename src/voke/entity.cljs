(ns voke.entity
  (:require [voke.schemas :refer [Brain Entity Weapon]])
  (:require-macros [schema.core :as sm]))

(defonce next-entity-id (atom 0))

(defn get-next-entity-id []
  (let [id-to-return @next-entity-id]
    (swap! next-entity-id inc)
    id-to-return))

(sm/defn make-entity :- Entity
  [entity-map]
  (assoc entity-map :id (get-next-entity-id)))

(sm/defn make-player-brain :- Brain
  []
  {:type                    :player
   :intended-move-direction #{}
   :intended-fire-direction []})

(sm/defn make-weapon :- Weapon
  []
  {:last-attack-timestamp 0
   :projectile-shape {:type :rectangle
                      :width 10
                      :height 10}})

;; Public

(sm/defn player :- Entity
  [x y]
  (make-entity
    {:position   {:x x :y y}
     :shape      {:width 25 :height 25 :type :rectangle :orientation 0}
     :motion     {:velocity             {:x 0 :y 0}
                  :affected-by-friction true
                  :max-acceleration     2.0
                  :max-speed            11}
     :collision  {:type :good-guy}
     :renderable true
     :weapon     (make-weapon)
     :brain      (make-player-brain)}))

(sm/defn wall :- Entity
  [x y width height]
  (make-entity
    {:position   {:x x :y y}
     :shape      {:width width :height height :type :rectangle :orientation 0}
     :collision  {:type :obstacle}
     :renderable true}))

(sm/defn projectile :- Entity
  [owner-id position projectile-shape orientation x-velocity y-velocity]
  (make-entity
    {:position   position
     :shape      (assoc projectile-shape :orientation orientation)
     :owner-id   owner-id
     :collision  {:type                 :projectile
                  ; XXXX TODO parameterize good/bad guy based on projectile owner
                  :collides-with        #{:bad-guy :obstacle :item}
                  :destroyed-on-contact true}
     :renderable true
     :motion     {:velocity             {:x x-velocity
                                         :y y-velocity}
                  :affected-by-friction false
                  :max-speed            (max x-velocity y-velocity)
                  :max-acceleration     0}}))
