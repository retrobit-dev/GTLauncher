const assert=require('node:assert/strict'),b=require('../preview/bridge-geometry.js'),path=require('../preview/gameplay-path.js');
for(const axis of ['E','S'])for(const material of ['wood','stone']) {
 const bridge={column:5,row:5,axis,material},c=b.cells(bridge),p=(x,y)=>({column:x,row:y});assert.equal(c.length,3);
 assert(b.permitsStep([bridge],c[0],c[1]));assert(b.permitsStep([bridge],c[1],c[2]));
 const start=axis==='E'?p(4,5):p(5,4),end=axis==='E'?p(8,5):p(5,8),side=axis==='E'?p(6,4):p(4,6);
 assert(b.permitsStep([bridge],start,c[0]));assert(b.permitsStep([bridge],c[2],end));assert(!b.permitsStep([bridge],c[1],side));assert(!b.permitsStep([bridge],side,c[1]));
}
assert.equal(b.cells({column:1,row:1,axis:'W',material:'wood'}),null);
const w={gridSize:20,tiles:Array(400).fill(1),blockStacks:Array.from({length:400},()=>[1])};w.blockStacks[0]=[7,8];w.tiles[0]=8;
assert(path.find(w,{column:3,row:3},{column:4,row:3},null),'new grass-topped blocks must not disable stacked-world pathfinding');
console.log('Bridge footprint and railing rules; v14 stacked pathfinding: OK');
{
 const world={gridSize:20,tiles:Array(400).fill(1)}, bridge={column:5,row:5,axis:'E',material:'wood'};
 world.tiles[5*20+6]=3;assert(b.valid(world,[bridge]),'dry ends and water in the middle');
 assert(!b.valid(world,[bridge,bridge]),'overlapping bridges');
 world.tiles[5*20+5]=3;assert(!b.valid(world,[bridge]),'water at entrance');world.tiles[5*20+5]=1;
 world.orangeTrees=[{column:6,row:5,kind:'wild-grass',size:1}];assert(!b.valid(world,[bridge]));delete world.orangeTrees;
 world.fences=[{column:6,row:4,edge:'S',material:'wood'}];assert(!b.valid(world,[bridge]),'canonical adjacent fence conflicts');delete world.fences;
 world.blockStacks=world.tiles.map(t=>[t]);world.blockStacks[5*20+6]=[1,3];assert(!b.valid(world,[bridge]),'uneven ground');
}
console.log('Bridge placement rejects overlap, water entrances, obstructions and uneven terrain: OK');
