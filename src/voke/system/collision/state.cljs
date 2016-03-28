(ns voke.system.collision.state
  "Contains atoms used to track the collision system's internal state.
  The contents of these atoms are reset at the beginning of every frame.")

; A set of entity IDs. When a :destroyed-on-contact entity is involved in a collision,
; its ID is stored in this set so that the collision system knows to discard any (attempt-to-move!) calls
; related to this entity for the rest of the frame.
(defonce dead-entities (atom #{}))

; A set of pairs of entity IDs. When a :contact event is fired to record that two entities have touched
; each other, we store their IDs in this atom in order to dedupe contacts for the rest of the frame.
; For instance, imagine a situation where entity 0 (a player) and entity 1 (a monster) both attempt to
; move into each other in the same frame. We don't want to fire two separate :contact events, because
; downstream systems (eg the damage system) would do duplicate work, and the player would eg take two ticks
; of damage within the same frame, rather than one.
(defonce contacts-fired (atom #{}))
