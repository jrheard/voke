(function(window) {

var entitiesByID = {};

var treeItemsByID = {};

var tree = rbush(15);

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

    shapeToTreeItem: function(shape, entityID) {
        return [
            this.leftEdgeX(shape),
            this.topEdgeY(shape),
            this.rightEdgeX(shape),
            this.bottomEdgeY(shape),
            {id: entityID}
        ];
    },

    insertIntoTree: function(entity) {
        var treeItem = (this.shapeToTreeItem(entity.shape, entity.id));
        tree.insert(treeItem);
        treeItemsByID[entity.id] = treeItem;
    },

    removeFromTree: function(entity) {
        tree.remove(treeItemsByID[entity.id]);
        delete treeItemsByID[entity.id];
    },

    resetState: function() {
        tree = rbush(15);
    },

    addEntity: function(entity) {
        this.insertIntoTree(entity);
        entitiesByID[entity.id] = entity;
    },

    updateEntity: function(entityID, newCenter) {
        this.removeFromTree(entitiesByID[entityID]);
        entitiesByID[entityID].shape.center = newCenter;
        this.insertIntoTree(entitiesByID[entityID]);
    },

    getEntityCenter: function(entityID) {
        return entitiesByID[entityID].shape.center;
    },

    removeEntity: function(entityID) {
        this.removeFromTree(entitiesByID[entityID]);
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

    findContactingEntityIDs: function(entityID, newCenter) {
        var movingEntity = entitiesByID[entityID];

        var newShape = this.shallowCopy(movingEntity.shape);
        newShape.center = newCenter;

        var relevantTreeItems = tree.search(
            this.shapeToTreeItem(newShape, entityID)
        );

        var relevantEntityIDs = relevantTreeItems.map(function(aTreeItem) {
            return aTreeItem[4].id;
        });

        return relevantEntityIDs.filter(function(anEntityID) {
            // XXX HACK - sometimes a figwheel reload will cause situations where
            // entitiesByID[anEntityID] is null. can't figure out why. :(
            return entitiesByID[anEntityID] &&
                this.entitiesCanCollide(movingEntity, entitiesByID[anEntityID]);
        }.bind(this));
    }

};

window.Collision = Collision;

})(window);
