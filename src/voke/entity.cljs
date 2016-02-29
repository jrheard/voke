(ns voke.entity
  (:require [voke.schemas :refer [Entity]])
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

(sm/defn player :- Entity
  [x y]
  (make-entity
    {:position                {:x x :y y}
     :collision-box           {:width 25 :height 25}
     :render-info             {:shape :rect}
     :human-controlled        true
     :intended-move-direction #{}
     ; TODO make fire direction be an ordered set
     :intended-fire-direction #{}}))

(sm/defn wall :- Entity
  [x y width height]
  (make-entity
    {:position      {:x x :y y}
     :collision-box {:width width :height height}
     :render-info   {:shape :rect}}))
