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

    updateEntity: function(entityID, newCenter) {
        entitiesByID[entityID].shape.center = newCenter;
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

    findContactingEntityID: function(entityID, newCenter) {
        var movingEntity = entitiesByID[entityID];

        var allEntities = [];
        for (var id in entitiesByID) {
            allEntities.push(entitiesByID[id]);
        }

        var collidableEntities = allEntities.filter(function(anotherEntity) {
            return this.entitiesCanCollide(movingEntity, anotherEntity);
        }.bind(this));

        var newShape = this.shallowCopy(movingEntity.shape);
        newShape.center = newCenter;

        var collidingEntities = collidableEntities.filter(function(anotherEntity) {
            return this.shapesCollide(newShape, anotherEntity.shape);
        }.bind(this));

        return collidingEntities.map(function(entity){
            return entity.id
        });
    }

};

window.Collision = Collision;

})(window);
