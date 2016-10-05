(ns voke.system.collision-test-macros
  (:require [clojure.spec :as s]))

(defmacro with-collision-env
  "docstring"
  [entities collision-events-sym apply-movement-calls-sym game-state-sym & body]
  `(let [~collision-events-sym (atom [])
         ~apply-movement-calls-sym (atom [])
         ~game-state-sym (assoc voke.test-utils/blank-game-state
                           :game-state/entities
                           (into {}
                                 (map (juxt :entity/id identity) ~entities)))]

     (doseq [entity# ~entities]
       (collision-util/-track-entity entity#))

     (events/subscribe-to-event :contact
                                (fn [event#]
                                  (swap! ~collision-events-sym conj event#)))

     (with-redefs [state/contacts-fired (atom #{})
                   collision-util/apply-movement (fn [& args#]
                                                   (swap! ~apply-movement-calls-sym conj args#))]

       ~@body)

     (js/Collision.resetState)))

(s/fdef with-collision-env
        :args (s/cat :entities vector?
                     :collision-events-sym simple-symbol?
                     :apply-movement-calls-sym simple-symbol?
                     :game-state-sym simple-symbol?
                     :body (s/* any?)))
