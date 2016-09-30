(ns voke.state
  "Contains functions that let Systems express an intent to modify the state of the game.

  Systems can call add-entity!, update-entity!, and remove-entity! in their tick functions / event handlers.
  These updates/removes will be queued, and will be processed by the core game loop in voke.core
  at the end of every frame."
  (:require [cljs.spec :as s]
            [voke.events :refer [publish-event]]))

;; Specs

(s/def :game-state/entities (s/coll-of :entity/entity))
(s/def :game-state/game-state (s/keys :req [:game-state/entities]))

(s/def :game-state-event/type #{:add :remove :update})
(s/def :game-state-event/origin keyword?)
(s/def :game-state-event/entity :entity/entity)
(s/def :game-state-event/entity-id :entity/id)
(s/def :game-state-event/update-fn fn?)

(def -base-event-keys [:game-state-event/type :game-state-event/origin])
(defmulti event-type :game-state-event/type)
(defmethod event-type :add [_]
  (s/keys :req (conj -base-event-keys :game-state-event/entity)))
(defmethod event-type :update [_]
  (s/keys :req (conj -base-event-keys :game-state-event/entity-id :game-state-event/update-fn)))
(defmethod event-type :remove [_]
  (s/keys :req (conj -base-event-keys :game-state-event/entity-id)))

(s/def :game-state-event/event (s/multi-spec event-type :game-state-event/type))

(defn- event-with-type [event-type]
  (s/and :game-state-event/event
         #(= (% :game-state-event/type) event-type)))

;; Private

(def ^:private buffer (atom []))

(defn process-events
  [state
   event-processing-fn
   events]
  (assoc state
         :entities
         (persistent!
           (reduce event-processing-fn
                   (transient (state :entities))
                   events))))

(s/fdef process-events
  :args (s/cat :state :game-state/game-state
               :event-processing-fn fn?
               :events (s/coll-of :game-state-event/event)))

(defn process-add-events
  [state
   add-events]
  (process-events state
                  (fn [entities event]
                    (let [entity (event :entity)]
                      (publish-event {:type   :entity-added
                                      :entity entity})
                      (assoc! entities (entity :id) entity)))
                  add-events))

(s/fdef process-add-events
  :args (s/cat :state :game-state/game-state
               :add-events (s/coll-of (event-with-type :add))))

(defn process-update-events
  [state
   update-events]
  (process-events state
                  (fn [entities event]
                    (let [entity-id (event :entity-id)]
                      (assoc! entities
                              entity-id
                              ((event :fn) (get entities entity-id)))))
                  update-events))

(s/fdef process-update-events
  :args (s/cat :state :game-state/game-state
               :update-events (s/coll-of (event-with-type :update))))

(defn process-remove-events
  [state
   remove-events]
  (process-events state
                  (fn [entities remove-event]
                    (let [entity-id (remove-event :entity-id)]
                      (publish-event {:type      :entity-removed
                                      :entity-id entity-id})
                      (dissoc! entities entity-id)))
                  remove-events))

(s/fdef process-remove-events
  :args (s/cat :state :game-state/game-state
               :remove-events (s/coll-of (event-with-type :remove))))

;; Public (but only intended to be used by voke.core)

(defn make-game-state
  [entities]
  {:game-state/entities (into {}
                              (map (juxt :entity/id identity)
                                   entities))})

(s/fdef make-game-state
  :args (s/cat :entities (s/coll-of :entity/entity))
  :ret :game-state/game-state)

(defn flush!
  "Takes a :game-state/game-state and returns a :game-state/game-state to which all queued
  :game-state-event/events have been applied.

  Resets @buffer to []."
  [state]
  (let [buffer-contents @buffer
        add-events (filter #(= (% :type) :add) buffer-contents)
        update-events (filter #(= (% :type) :update) buffer-contents)
        remove-events (filter #(= (% :type) :remove) buffer-contents)]
    (reset! buffer [])

    (-> state
        (process-add-events add-events)
        (process-update-events update-events)
        (process-remove-events remove-events))))

(s/fdef flush!
  :args (s/cat :state :game-state/game-state)
  :ret :game-state/game-state)

;; Public

(defn add-entity!
  [entity
   origin]
  (swap! buffer conj {:type   :add
                      :origin origin
                      :entity entity}))

(s/fdef add-entity!
  :args (s/cat :entity :entity/entity
               :origin keyword?))

(defn update-entity!
  "Queue an update to a particular entity.

  Mainly intended to be used by systems' event handlers. System tick functions should return modified entities
  rather than calling this function."
  [entity-id
   origin
   update-fn]
  ; this swap! call takes a bit longer than i'd like, shows up in profiles at around 3% of
  ; the time we spend. consider replacing the buffer with (gasp) a regular js array. not worth it yet though
  (swap! buffer conj {:type      :update
                      :origin    origin
                      :entity-id entity-id
                      :fn        update-fn}))

(s/fdef update-entity!
  :args (s/cat :entity-id :entity/id
               :origin keyword?
               :update-fn fn?))

(defn remove-entity!
  "Queue an entity's removal from the game."
  [entity-id
   origin]
  (swap! buffer conj {:type      :remove
                      :origin    origin
                      :entity-id entity-id}))

(s/fdef remove-entity!
  :args (s/cat :entity-id :entity/id
               :origin keyword?))
