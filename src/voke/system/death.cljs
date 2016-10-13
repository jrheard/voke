(ns voke.system.death
  (:require [cljs.spec :as s]
            [voke.specs]
            [voke.state :as state]))

(defn dead? [entity]
  (and (contains? entity :component/health)
       (<= (get-in entity [:component/health :health/health])
           0)))

(s/fdef dead?
  :args (s/cat :entity :entity/entity)
  :ret boolean?)

(def system {:system/tick-fn (fn [entities]
                               (doseq [entity (filter dead? entities)]
                                 (state/remove-entity! (entity :entity/id) :death-system)))})
