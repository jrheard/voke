(ns voke.system.rendering
  (:require [cljsjs.pixi]
            [voke.schemas :refer [Entity System]])
  (:require-macros [schema.core :as sm]))

; ok. here is how things have to work
; there are a few situations we need to account for
; 1) level start - lots of entities exist, none (or maybe some!) have moved
; 2) preexisting entity moves
; 3) new entity is created
; 4) old entity is removed
; so we need to listen to :entity-created, :entity-removed, and also :movement.
; and we need to be able to detect situations where there's an entity that we don't yet render

(defn handle-unknown-entities! [stage objects-by-entity-id entities]
  (doseq [entity entities]
    (let [obj (doto (js/PIXI.Graphics.)
                (.beginFill 0xEEEEEE)
                (.drawRect 0
                           0
                           (-> entity :collision-box :width)
                           (-> entity :collision-box :height))
                (.endFill)
                (aset "x" (-> entity :position :x))
                (aset "y" (-> entity :position :y)))]
      (.addChild stage obj)
      (swap! objects-by-entity-id assoc (:id entity) obj))))

(sm/defn render-system-tick [renderer stage objects-by-entity-id entities publish-chan]
  (let [unknown-entities (filter #(not (contains? @objects-by-entity-id
                                                  (:id %)))
                                 entities)]
    (handle-unknown-entities! stage objects-by-entity-id unknown-entities)

    (.render renderer stage))
  entities)

(defn a-rendering-event-handler [stage objects-by-entity-id event]
  (if-let [obj (@objects-by-entity-id (-> event :entity :id))]
    (do
      (doto obj
       ; TODO clean up all this code obviously
       (aset "x" (-> event :entity :position :x))
       (aset "y" (-> event :entity :position :y))))
    (handle-unknown-entities! stage objects-by-entity-id [(event :entity)])))

(defonce ^:private -rendering-engine
         {:renderer             (js/PIXI.autoDetectRenderer. 1000 700)
          :stage                (js/PIXI.Container.)
          :objects-by-entity-id (atom {})})

(def render-system
  (let [{:keys [renderer stage objects-by-entity-id]} -rendering-engine]

    (.appendChild js/document.body (.-view renderer))

    {:every-tick     {:reads #{:position :render-info}
                      :fn    (fn [& args]
                               (apply render-system-tick renderer stage objects-by-entity-id args))}
     :event-handlers [{:event-type :movement
                       :fn         #(a-rendering-event-handler stage objects-by-entity-id %)}]}))
