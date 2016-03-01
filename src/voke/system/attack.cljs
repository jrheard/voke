(ns voke.system.attack
  (:require [schema.core :as s]
            [voke.entity :refer [projectile]]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity System]])
  (:require-macros [schema.core :as sm]))

(sm/defschema AttackState {:last-attack-timestamp (s/Int)})

(sm/defn make-attack-state :- AttackState
  []
  {:last-attack-timestamp nil})

(sm/defn can-attack? :- s/Bool
  [attack-state :- AttackState
   entity :- Entity]
  ; TODO
  true)

(sm/defn process-firing-entities :- [Entity]
  [attack-state
   entities :- [Entity]
   publish-chan]
  (let [attack-state @attack-state]
    (for [entity (filter #(can-attack? attack-state %)
                         entities)]
      (let [{:keys [x y]} (entity :shape)]
        ; UGH UGH UGH TODO can't implement this until velocity/acceleration are done
        (projectile x y 10 10))
      )))

;; System definition

(sm/def attack-system :- System
  (let [attack-state (atom {})]
    {:every-tick {:reads #{:intended-fire-direction}
                  :fn    (fn [& args] (apply process-firing-entities attack-state args))}}))
