(ns voke.system.damage
  "Responsible for listening to :contact events and firing :damage events."
  (:require [cljs.spec :as s]
            [voke.specs]
            [voke.events :refer [publish-event]]))

(defn one-way-damage-check
  "Checks to see if entity a can damage entity b, and fires a :damage event if so."
  [a b]
  (when (and (contains? a :component/damage)
             (contains? b :component/health))
    (publish-event {:event/type :damage
                    :source     a
                    :target     b
                    :amount     (get-in a [:component/damage :damage/amount])})))

(s/fdef one-way-damage-check :args (s/cat :a :entity/entity
                                          :b :entity/entity))

(defn handle-contact
  [event]
  (let [[a b] (event :entities)]
    (one-way-damage-check a b)
    (one-way-damage-check b a)))

(def system {:system/event-handlers [{:event/type              :contact
                                      :system/event-handler-fn handle-contact}]})
