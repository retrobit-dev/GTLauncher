const assert = require('node:assert/strict');
const path = require('../preview/gameplay-path.js');
const size=20, tiles=Array(size*size).fill(0), stacks=Array.from({length:size*size},()=>[]);
function put(x,y,s){tiles[y*size+x]=s.at(-1)||0;stacks[y*size+x]=s;}
put(1,1,[1]);put(2,1,[1,2]);put(3,1,[1,2,4]);put(4,1,[1,2,4,5,1]);
const world={gridSize:size,tiles,blockStacks:stacks};
const a={column:1,row:1}, b={column:3,row:1}, c={column:4,row:1};
assert.equal(path.find(world,a,b,null).length,3,'can ascend one level at a time');
assert.equal(path.find(world,b,a,null).length,3,'can descend one level at a time');
assert.equal(path.find(world,b,c,null),null,'two-level cliff is blocked');
put(3,2,[1,2,4,5]);put(4,2,[1,2,4,5]);
assert.equal(path.find(world,b,c,null).length,4,'route detours via one-level steps');
assert.equal(path.find({...world,blockStacks:[]},a,b,null),null,'invalid elevation fails closed');
assert.equal(path.find({gridSize:size,tiles},a,c,null).length,4,'legacy flat world remains routable');
console.log('Elevation-aware pathfinding and legacy flat routes: OK');
