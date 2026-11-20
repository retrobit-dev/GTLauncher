const assert=require('node:assert/strict'),path=require('../preview/gameplay-path');
const size=20, tiles=Array(size*size).fill(0),set=(x,y,t)=>tiles[y*size+x]=t;
set(5,5,3);set(6,5,3);set(7,5,1);set(5,6,1);
const world={gridSize:size,tiles};
assert.deepEqual(path.findNearestDry(world,{column:5,row:5},null),[{column:5,row:5},{column:5,row:6}]);
set(5,6,0);assert.deepEqual(path.findNearestDry(world,{column:5,row:5},null).at(-1),{column:7,row:5});
set(7,5,0);assert.equal(path.findNearestDry(world,{column:5,row:5},null),null);
console.log('Nearest reachable dry route and no-exit handling: OK');
