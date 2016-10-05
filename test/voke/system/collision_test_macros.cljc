(ns voke.system.collision-test-macros
  (:require [clojure.spec :as s]))

; I know you're only supposed to write macros that start with def- or let-,
; and that having your macro introduce new symbols to your environment like this is gross,
; but this is just a test utility helper that nobody else will ever look at so I don't feel bad.

(defmacro with-collision-env
  "Sets up / tears down the environment necessary for collision-related tests to run.
  See voke.system.collision-test for example usage."
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
