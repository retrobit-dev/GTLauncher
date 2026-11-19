const assert=require('node:assert/strict');
const {key,blocks}=require('../preview/fence-edges.js');
const east={column:4,row:5,edge:'E'},west={column:5,row:5,edge:'W'};
assert.equal(key(east),key(west));
assert.equal(key({column:4,row:5,edge:'S'}),key({column:4,row:6,edge:'N'}));
for(const fence of [east,west]) {
 assert(blocks(fence,{column:4,row:5},{column:5,row:5}));
 assert(blocks(fence,{column:5,row:5},{column:4,row:5}));
 assert(!blocks(fence,{column:4,row:5},{column:4,row:6}));
 assert(!blocks(fence,{column:4,row:5},{column:5,row:6}));
}
assert.equal(key({column:4,row:5,edge:'invalid'}),null);
console.log('Canonical fence edge and bidirectional movement checks: OK');
const {find}=require('../preview/gameplay-path.js');
const w={gridSize:20,tiles:Array(400).fill(1),fences:[{column:2,row:2,edge:'N',material:'wood'},{column:2,row:2,edge:'E',material:'stone'},{column:2,row:2,edge:'S',material:'wood'},{column:2,row:2,edge:'W',material:'stone'}]};
assert.equal(find(w,{column:2,row:2},{column:3,row:2},null),null,'enclosed player cannot cross fences');
w.fences.pop();const route=find(w,{column:2,row:2},{column:3,row:2},null);assert(route && route.length>2,'route uses opening instead of crossing fence');
console.log('Fence pathfinding enclosure and opening: OK');
