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
    ; TODO - the shapes in this file don't have :angles
    {:shape                   {:x x :y y :width 25 :height 25 :type :rectangle}
     :collision               {:type :player}
     :renderable              true
     :human-controlled        true
     :intended-move-direction #{}
     ; TODO make fire direction be an ordered set
     :intended-fire-direction #{}}))

(sm/defn wall :- Entity
  [x y width height]
  (make-entity
    {:shape      {:x x :y y :width width :height height :type :rectangle}
     :collision  {:type :obstacle}
     :renderable true}))
