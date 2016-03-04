(ns voke.entity
  (:require [voke.schemas :refer [Brain Entity]])
  (:require-macros [schema.core :as sm]))

(defonce next-entity-id (atom 0))

(defn get-next-entity-id []
  (let [id-to-return @next-entity-id]
    (swap! next-entity-id inc)
    id-to-return))

(sm/defn make-entity :- Entity
  [entity-map]
  (assoc entity-map :id (get-next-entity-id)))

;; Public

(sm/defn make-player-brain :- Brain
  []
  {:type                    :player
   :intended-move-direction #{}
   :intended-fire-direction #{}})

(sm/defn player :- Entity
  [x y]
  (make-entity
    {:shape      {:x x :y y :width 25 :height 25 :type :rectangle :orientation 0}
     :motion     {:velocity         {:x 0 :y 0}
                  :max-acceleration 2.0
                  :max-speed        11}
     :collision  {:type :player}
     :renderable true
     :brain      (make-player-brain)}))

(sm/defn wall :- Entity
  [x y width height]
  (make-entity
    {:shape      {:x x :y y :width width :height height :type :rectangle :orientation 0}
     :collision  {:type :obstacle}
     :renderable true}))
