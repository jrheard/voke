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

(sm/defn render-system-tick [renderer stage entities publish-chan]
  ;(let [unknown-entities ]) ; TODO add some sort of map of entitiy id to sprite or whatever
  ; and detect if there are any entities in `entities` that aren't in our map atom
  (.render renderer stage)
  entities)

(defn a-rendering-event-handler [event]
  ; TODO event has an :entity
  (js/console.log (clj->js event)))

(defonce ^:private rendering-engine
         {:renderer (js/PIXI.autoDetectRenderer. 800 600)
          :stage    (js/PIXI.Container.)
          ; TODO likely also an atom mapping entity-id to sprite-or-equivalent js object
          })

(def render-system
  (let [{:keys [renderer stage]} rendering-engine]

    (.appendChild js/document.body (.-view renderer))

    {:every-tick     {:reads #{:position :render-info}
                      :fn    (fn [& args]
                               (apply render-system-tick renderer stage args))}
     :event-handlers [{:event-type :movement
                       :fn         a-rendering-event-handler}]}))
