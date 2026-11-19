const assert=require('node:assert/strict'),jump=require('../preview/jump-motion.js'),path=require('../preview/gameplay-path.js');
for(const [a,b] of [[1,2],[2,1],[10,11],[11,10]]){
 assert.equal(jump.height(a,b,0),0);assert(Math.abs(jump.height(a,b,1))<1e-10);assert.equal(jump.height(a,b,.5),3);
 for(const t of [.1,.3,.7,.9])assert(Math.abs(jump.height(a,b,t)-jump.height(b,a,1-t))<1e-9);
}
for(const [a,b] of [[1,1],[1,3],[3,1],[0,1]])assert.equal(jump.height(a,b,.5),0);
assert.equal(jump.height(1,2,NaN),0);
const stacks=Array.from({length:400},()=>[]);stacks[2*20+2]=[1];stacks[2*20+3]=[1,1];stacks[2*20+4]=[1,1,1,1];const world={gridSize:20,tiles:stacks.map(x=>x.length?1:0),blockStacks:stacks};
assert.deepEqual(path.find(world,{column:2,row:2},{column:3,row:2}),[{column:2,row:2},{column:3,row:2}]);
assert.equal(path.find(world,{column:3,row:2},{column:4,row:2}),null);assert.equal(path.find(world,{column:4,row:2},{column:3,row:2}),null);
console.log('One-level ascent/descent jump arc, reverse continuity and two-level rejection: OK');
