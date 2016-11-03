(ns voke.system.camera
  (:require [voke.events]
            [voke.system.rendering :as rendering]))

(defn handle-movement
  [event]
  (when (contains? (event :entity) :component/input)
    (rendering/update-camera-position! (get-in event [:entity :component/shape :shape/center]))))

(def system {:system/event-handlers [{:event/type              :movement
                                      :system/event-handler-fn handle-movement}]})
