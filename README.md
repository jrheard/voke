# voke

[![CircleCI](https://circleci.com/gh/jrheard/voke.svg?style=svg)](https://circleci.com/gh/jrheard/voke)
[![Dependencies Status](https://jarkeeper.com/jrheard/voke/status.svg)](https://jarkeeper.com/jrheard/voke)

a roguelike

http://jrheard.com/voke usually has a reasonably-recent, reasonably-stable version

## Setup

`lein figwheel`, then visit `http://localhost:3449/`

## Implementation Notes (work in progress)

I have no idea what I'm doing.

I figured it'd be fun to build a game from scratch, because I don't know how to do that, and
because I want to figure out how to write a nontrivial ClojureScript program. The general idea
is to have gameplay similar to Binding of Isaac, with sprawling randomly generated levels vaguely (vaguely! vaguely vaguely vaguely) in the
style of Diablo, and a super-simple leveling-up system like Risk of Rain. (Not the difficulty-increases-over-time system,
just the super-basic your-guy-gets-a-little-more-powerful-when-he-kills-a-bunch-of-monsters
system.)

I don't intend for anyone else to ever play this game, I just figured it'd be fun to figure out
how to write a computer program program like this one.

I'm using what I *think* is a reasonable incarnation of the Entity/Component/System pattern.
I'd never heard of this before, but apparently it's a useful way of writing video games. My notes
in `dev-diary.txt` are pretty stream-of-consciousness, so I'm not sure exactly which articles were
my favorites when I was researching this stuff, but I think these were some of the better ones:

* http://t-machine.org/index.php/2007/09/03/entity-systems-are-the-future-of-mmog-development-part-1/
* http://www.richardlord.net/blog/what-is-an-entity-framework
* http://t-machine.org/index.php/2013/05/30/designing-bomberman-with-an-entity-system-which-components/

It's a super-declarative approach and allows for really great composition / reuse /
separation of concerns. Everything in the game - monsters, walls, projectiles, items, loot, whatever -
is an Entity, which is basically just an integer ID; and each Entity can have zero or more Components, which
are just simple bags of data that say something about the entity's state, and implicitly about how it behaves.

I've seen most other people use classes, records, etc for this, but I just used maps, because I like maps.

For instance, here's what the player character looks like:

```
{:component/shape
 {:shape/width 25,
  :shape/height 25,
  :shape/type :rectangle,
  :shape/center {:geometry/x 100, :geometry/y 150}},
 :component/motion
 {:motion/velocity {:geometry/x 0, :geometry/y 0},
  :motion/affected-by-friction true,
  :motion/direction nil,
  :motion/max-acceleration 2,
  :motion/max-speed 11},
 :component/collision {:collision/type :good-guy},
 :component/render {:render/fill 3355443},
 :component/weapon
 {:weapon/last-attack-timestamp 0,
  :weapon/fire-direction nil,
  :weapon/shots-per-second 21,
  :weapon/shot-speed 5,
  :weapon/projectile-color 6710886,
  :weapon/projectile-shape
  {:shape/type :rectangle, :shape/width 10, :shape/height 10}},
 :component/input
 {:input/intended-move-direction #{:up :right},
  :input/intended-fire-direction [:down :up :right]},
 :entity/id 241}
```

And here's what a bullet that the player just fired looks like:

```
{:component/shape
 {:shape/type :rectangle,
  :shape/width 10,
  :shape/height 10,
  :shape/center {:geometry/x 117.5, :geometry/y 150}},
 :component/owned {:owned/owner-id 241},
 :component/collision
 {:collision/type :projectile,
  :collision/collides-with #{:item :obstacle :bad-guy},
  :collision/destroyed-on-contact true},
 :component/render {:render/fill 6710886},
 :component/motion
 {:motion/velocity {:geometry/x 5, :geometry/y 0},
  :motion/direction 0,
  :motion/affected-by-friction false,
  :motion/max-speed 5,
  :motion/max-acceleration 0},
 :entity/id 242}
```

So, that's Entities and Components. They don't really do anything, they're just data.

Systems are what make the game actually work. There's a collision system, a movement system,
an input system, etc. Most of them work by defining a "tick function" that's run once per frame;
it takes as input the list of all the entities in the game, picks out whatever entities it's
interested in, and a) returns updated versions of those entities and/or b) performs some
side effect, like adding or deleting an entity.

Systems communicate with each other via a simple event system in `voke.events`, so e.g.
the collision system can fire `:contact` events, which the damage system listens to so it can
figure out when a projectile hit a target, or the inventory system can listen to so it can
figure out when the player moved over some gold or an item, etc, and the collision system
doesn't have to know that either of those other systems exists.

In addition to just returning updated entities in their tick function, systems can also
modify the state of the game by using the `add-entity!`, `update-entity!`, and `remove-entity!`
functions located in `voke.state`. This is useful e.g. in event handlers that listen to that `voke.events`
system described in the previous paragraph.

That's about it. The game isn't very complicated yet, so right now you could say that it's
pretty overengineered; I'm pretty satisfied with this setup so far, though. Several times now,
I've sat down to add a new feature and it's been really straightforward and basically worked
the first time (e.g. the AI system, which lets monsters chase and fire weapons at the player).
Each new system can just kinda do its own thing without having to worry about breaking any of
the other systems, and so adding / modifying features has been really simple so far.
We'll see if it stays that way!

## License

[MIT License](http://choosealicense.com/licenses/mit/)
