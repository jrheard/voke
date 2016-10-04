(ns voke.system.movement
  (:require [cljs.spec :as s]
            [voke.system.collision.system :refer [attempt-to-move!]]))

; A dumb-as-rocks velocity/acceleration system.
; Based on http://www.randygaul.net/2012/02/22/basic-2d-vector-physics-acceleration-orientation-and-friction/ .
;
; If I were a better man, I would have implemented http://buildnewgames.com/gamephysics/
; or http://blog.wolfire.com/2009/07/linear-algebra-for-game-developers-part-2/
;
; This system was easy to implement and I don't need all the bells/whistles in the linear-algebra-based
; articles, though, so I don't feel *too* bad.
; May have made collision resolution a bit more fiddly / less elegant than it could have been, though.

(def friction-value 0.80)
(def min-velocity 0.05)

(defn should-update-velocity?
  "Entities' velocity should be updated if they're intending to move somewhere
  or if they're currently moving and affected by friction."
  [entity]
  (or (get-in entity [:component/motion :motion/direction])
      (and
        (get-in entity [:component/motion :motion/affected-by-friction])
        (or
          (not= (get-in entity [:component/motion :motion/velocity :geometry/x]) 0)
          (not= (get-in entity [:component/motion :motion/velocity :geometry/y]) 0)))))

(s/fdef should-update-velocity?
  :args (s/cat :entity :entity/entity)
  :ret boolean?)

(defn get-acceleration
  [entity]
  (if (get-in entity [:component/motion :motion/direction])
    (get-in entity [:component/motion :motion/max-acceleration])
    0))

(s/fdef get-acceleration
  :args (s/cat :entity :entity/entity)
  :ret number?)

(defn -update-axis-velocity
  [entity axis trig-fn]
  (let [axis-velocity (get-in entity [:component/motion :motion/velocity axis])
        new-velocity (+ axis-velocity
                        (* (get-acceleration entity)
                           (trig-fn (get-in entity [:component/motion :motion/direction]))))]
    (if (> (Math/abs new-velocity) min-velocity)
      (assoc-in entity [:component/motion :motion/velocity axis] new-velocity)
      (assoc-in entity [:component/motion :motion/velocity axis] 0))))

(s/fdef -update-axis-velocity
  :args (s/and (s/cat :entity :entity/entity
                      :axis :geometry/axis
                      :trig-fn fn?)
               #(contains? (% :entity) :component/motion))
  :ret :entity/entity)

(defn cap-velocity
  [entity]
  (let [velocity (get-in entity [:component/motion :motion/velocity])
        x-velocity (velocity :geometry/x)
        y-velocity (velocity :geometry/y)
        max-speed (get-in entity [:component/motion :motion/max-speed])
        amplitude (Math/sqrt (+ (* x-velocity x-velocity)
                                (* y-velocity y-velocity)))
        multiplier (if (> amplitude max-speed)
                     (/ max-speed amplitude)
                     1)]
    (assoc-in entity
              [:component/motion :motion/velocity]
              {:geometry/x (* x-velocity multiplier)
               :geometry/y (* y-velocity multiplier)})))

(s/fdef cap-velocity
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/motion))
  :ret :entity/entity)

(defn update-velocity
  [entity]
  (if (should-update-velocity? entity)
    (-> entity
        (-update-axis-velocity :geometry/x Math/cos)
        (-update-axis-velocity :geometry/y Math/sin)
        cap-velocity)
    entity))

(s/fdef update-velocity
  :args (s/cat :entity :entity/entity)
  :ret :entity/entity)

(defn apply-friction
  [entity]
  (if (get-in entity [:component/motion :motion/affected-by-friction])
    (reduce
      (fn [entity axis]
        (update-in entity [:component/motion :motion/velocity axis] #(* % friction-value)))
      entity
      [:geometry/x :geometry/y])
    entity))

(s/fdef apply-friction
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/motion))
  :ret :entity/entity)

(defn update-position
  [entity]
  (let [velocity (-> entity :component/motion :motion/velocity)
        center (-> entity :component/shape :shape/center)]
    (assoc-in entity
              [:component/shape :shape/center]
              {:geometry/x (+ (velocity :geometry/x) (center :geometry/x))
               :geometry/y (+ (velocity :geometry/y) (center :geometry/y))})))

(s/fdef update-position
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/motion)
               #(contains? (% :entity) :component/shape))
  :ret :entity/entity)

(defn relevant-to-movement-system?
  [entity]
  (or
    (get-in entity [:component/motion :motion/direction])
    (not= (get-in entity [:component/motion :motion/velocity :geometry/x] 0) 0)
    (not= (get-in entity [:component/motion :motion/velocity :geometry/y] 0) 0)))

(s/fdef relevant-to-movement-system?
  :args (s/cat :entity :entity/entity)
  :ret boolean?)

;; System definition

(def move-system
  {:system/tick-fn (fn move-system-tick [entities]
                     (doseq [entity (filter relevant-to-movement-system? entities)]
                       (let [moved-entity (-> entity
                                              update-velocity
                                              apply-friction
                                              update-position)]
                         (when (not= moved-entity entity)
                           (attempt-to-move! entity
                                             (-> moved-entity :component/shape :shape/center)
                                             (-> moved-entity :component/motion :motion/velocity)
                                             entities)))))})
