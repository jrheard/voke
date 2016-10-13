(ns voke.system.health
  (:require [voke.state :as state]))

(defn handle-damage [event]
  (state/update-entity! (-> event :target :entity/id)
                  :health-system
                  (fn [entity]
                    (update-in entity
                               [:component/health :health/health]
                               -
                               (event :amount)))))

(def system {:system/event-handlers [{:event/type              :damage
                                      :system/event-handler-fn handle-damage}]})
