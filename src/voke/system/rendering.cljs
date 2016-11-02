(ns voke.system.rendering
  (:require [cljs.spec :as s]
            [voke.pixi :as pixi :refer [render! update-entity-position! remove-entity!]]))

; TODO not yet implemented: removing entities (eg dead monsters, collided bullets, etc)
; TODO also what about when entities are no longer visible? should we remove their objects?
; watch perf first i guess and then see what happens
; (update: pixi forums say that yes, you should do this)

(defn handle-movement-event [event]
  (update-entity-position! (get-in event [:entity :entity/id]) (event :new-center)))

(defn handle-remove-entity-event [event]
  (remove-entity! (event :entity-id)))

;; Public

(defn update-camera-position!
  [position]
  (pixi/update-camera-position! (get position :geometry/x) (get position :geometry/y)))

(s/fdef update-camera-position!
  :args (s/cat :position :geometry/vector2))

(defn render-tick
  [state]
  (let [relevant-entities (filter #(and (contains? % :component/shape)
                                        (contains? % :component/render))
                                  (vals (state :game-state/entities)))]
    (render! relevant-entities)))

(s/fdef render-tick
  :args (s/cat :state :game-state/game-state))

;; System definition

(def system
  {:system/event-handlers [{:event/type              :movement
                            :system/event-handler-fn handle-movement-event}
                           {:event/type              :entity-removed
                            :system/event-handler-fn handle-remove-entity-event}]})
