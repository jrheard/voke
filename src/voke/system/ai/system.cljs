(ns voke.system.ai.system)

(def system
  {:system/tick-fn (fn [entities]
                     (let [player (->> entities
                                       (filter #(contains? % :component/input))
                                       first)
                           player-center (get-in player [:component/shape :shape/center])
                           entities-with-ai (filter #(contains? % :component/ai) entities)]

                       (for [entity entities-with-ai]
                         (let [entity-center (get-in entity [:component/shape :shape/center])
                               direction-to-player (Math/atan2 (- (player-center :geometry/y)
                                                                  (entity-center :geometry/y))
                                                               (- (player-center :geometry/x)
                                                                  (entity-center :geometry/x)))]
                           (-> entity
                               (assoc-in [:component/motion :motion/direction]
                                         nil
                                         ;direction-to-player
                                         )
                               (assoc-in [:component/weapon :weapon/fire-direction]
                                         ;direction-to-player
                                         ;0
                                         ;Math/PI
                                         ;(- (* 3 (/ Math/PI 4)))
                                         nil
                                         )
                               )))))})