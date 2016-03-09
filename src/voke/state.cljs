(ns voke.state
  (:require [schema.core :as s]
            [voke.schemas :refer [Entity EntityID GameState]])
  (:require-macros [schema.core :as sm]))

(sm/defn make-game-state :- GameState
  [entities :- [Entity]]
  {:entities (into {}
                   (map (juxt :id identity)
                        entities))})

(def ^:private buffer (atom []))

(sm/defn update-entity!
  [entity-id :- EntityID
   origin :- s/Keyword
   update-fn]
  (swap! buffer conj {:type      :update
                      :origin    origin
                      :entity-id entity-id
                      :fn        update-fn}))

(sm/defn remove-entity!
  [entity-id :- EntityID
   origin :- s/Keyword]
  (swap! buffer conj {:type      :remove
                      :origin    origin
                      :entity-id entity-id}))

(sm/defn flush!
  ; TODO document
  []
  (let [buffer-contents @buffer
        update-events (filter #(= (% :type) :update) buffer-contents)
        remove-events (filter #(= (% :type) :remove) buffer-contents)]
    (reset! buffer [])
    (sm/fn buffer-runner :- GameState
      [state :- GameState]
      (let [state-after-updates (reduce (fn [state update-event]
                                          (update-in state
                                                     [:entities (update-event :entity-id)]
                                                     (update-event :fn)))
                                        state
                                        update-events)]
        (reduce (fn [state remove-event]
                  (update-in state [:entities] dissoc (remove-event :entity-id)))
                state-after-updates
                remove-events)))))
