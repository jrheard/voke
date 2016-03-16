(ns voke.state
  "Contains functions that let Systems express an intent to modify the state of the game.

  Systems can call update-entiy! and remove-entity! in their tick functions / event handlers.
  These updates/removes will be queued, and will be processed by the core game loop in voke.core
  at the end of every frame."
  (:require [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity EntityID GameState]])
  (:require-macros [schema.core :as sm]))

(def ^:private buffer (atom []))

(sm/defn process-events
  [state :- GameState
   event-processing-fn
   events]
  (assoc state
         :entities
         (persistent!
           (reduce event-processing-fn
                   (transient (state :entities))
                   events))))

(sm/defn process-add-events :- GameState
  [state :- GameState
   add-events]
  (process-events state
                  (fn [entities event]
                    (let [entity (event :entity)]
                      (publish-event {:type   :entity-added
                                      :entity entity})
                      (assoc! entities (entity :id) entity)))
                  add-events))

(sm/defn process-update-events :- GameState
  [state :- GameState
   update-events]
  (process-events state
                  (fn [entities event]
                    (let [entity-id (event :entity-id)]
                      (assoc! entities
                              entity-id
                              ((event :fn) (get entities entity-id)))))
                  update-events))

(sm/defn process-remove-events :- GameState
  [state :- GameState
   remove-events]
  (process-events state
                  (fn [entities remove-event]
                    (let [entity-id (remove-event :entity-id)]
                      (publish-event {:type      :entity-removed
                                      :entity-id entity-id})
                      (dissoc! entities entity-id)))
                  remove-events))

;; Public (but only intended to be used by voke.core)

(sm/defn make-game-state :- GameState
  [entities :- [Entity]]
  {:entities (into {}
                   (map (juxt :id identity)
                        entities))})

(sm/defn flush!
  "Resets @buffer to [].

  Returns a function that <takes a GameState and returns a GameState to which all queued
  :update and :remove events have been applied>."
  []
  (let [buffer-contents @buffer
        add-events (filter #(= (% :type) :add) buffer-contents)
        update-events (filter #(= (% :type) :update) buffer-contents)
        remove-events (filter #(= (% :type) :remove) buffer-contents)]
    (reset! buffer [])

    (fn buffer-runner
      [state]
      (-> state
          (process-add-events add-events)
          (process-update-events update-events)
          (process-remove-events remove-events)))))

;; Public

(sm/defn add-entity!
  [entity
   origin :- s/Keyword]
  (swap! buffer conj {:type   :add
                      :origin origin
                      :entity entity}))

(sm/defn update-entity!
  "Queue an update to a particular entity."
  [entity-id :- EntityID
   origin :- s/Keyword
   update-fn]
  (swap! buffer conj {:type      :update
                      :origin    origin
                      :entity-id entity-id
                      :fn        update-fn}))

(sm/defn remove-entity!
  "Queue an entity's removal from the game."
  [entity-id :- EntityID
   origin :- s/Keyword]
  (swap! buffer conj {:type      :remove
                      :origin    origin
                      :entity-id entity-id}))
