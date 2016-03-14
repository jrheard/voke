(function(window) {

var entitiesByID = {};

var Collision = {
    shallowCopy: function(obj) {
        var newObj = {};
        for(var i in obj) {
            if(obj.hasOwnProperty(i)) {
                newObj[i] = obj[i];
            }
        }
        return newObj;
    },

    addEntity: function(entity) {
        entitiesByID[entity.id] = entity;
    },

    updateEntity: function(entityID, axis, newPosition) {
        entitiesByID[entityID].shape.center[axis] = newPosition;
    },

    removeEntity: function(entityID) {
        delete entitiesByID[entityID];
    },

    // NOTE: MAKE SURE COLLISION SYSTEM IS NOTIFIED WHENEVER THE VALUES OF ANY OF THESE FIELDS CHANGE
    // (e.g. if somehow an entity's collides-with list changes mid-game, that's important to know!)
    oneWayCollidabilityCheck: function(a, b) {
        if (!a.collision || !b.collision) {
            return false;
        }

        if (a.id === b.id) {
            return false;
        }

        if (a.collision['collides-with']) {
            return a.collision['collides-with'].indexOf(b.collision.type) !== -1;
        }

        return true;
    },

    entitiesCanCollide: function (a, b) {
        return this.oneWayCollidabilityCheck(a, b) && this.oneWayCollidabilityCheck(b, a);
    },

    leftEdgeX: function(shape) {
        return shape.center.x - (shape.width / 2);
    },

    rightEdgeX: function(shape) {
        return shape.center.x + (shape.width / 2);
    },

    topEdgeY: function(shape) {
        return shape.center.y - (shape.height / 2);
    },

    bottomEdgeY: function(shape) {
        return shape.center.y + (shape.height / 2);
    },

    shapesCollide: function(a, b) {
        return !(
            this.bottomEdgeY(a) < this.topEdgeY(b) ||
            this.topEdgeY(a) > this.bottomEdgeY(b) ||
            this.leftEdgeX(a) > this.rightEdgeX(b) ||
            this.rightEdgeX(a) < this.leftEdgeX(b)
        );
    },

    findContactingEntityID: function(entityID, axis, new_position) {
        var movingEntity = entitiesByID[entityID];

        var allEntities = [];
        for (var id in entitiesByID) {
            allEntities.push(entitiesByID[id]);
        }

        var collidableEntities = allEntities.filter(function(anotherEntity) {
            return this.entitiesCanCollide(movingEntity, anotherEntity);
        }.bind(this));

        var newShape = this.shallowCopy(movingEntity.shape);
        newShape.center = {
            x: movingEntity.shape.center.x,
            y: movingEntity.shape.center.y
        };
        newShape.center[axis] = new_position;

        var collidingEntities = collidableEntities.filter(function(anotherEntity) {
            return this.shapesCollide(newShape, anotherEntity.shape);
        }.bind(this));

        var collidingEntity = collidingEntities[0];

        if (collidingEntity) {
            return collidingEntity.id;
        } else {
            return null;
        }
    }

};

window.Collision = Collision;

})(window);
// i'm cargo-culting this function()(window) style from https://github.com/ibdknox/ChromaShift/blob/master/js/game.js
// i have no idea why it's necessary. TODO: google+learn