const assert=require('node:assert/strict'),view=require('../preview/world-view-rotation.js'),path=require('../preview/gameplay-path.js');
const size=20,tiles=Array(size*size).fill(0),blockStacks=tiles.map(()=>[]);
function put(c,r,blocks){blockStacks[r*size+c]=blocks;tiles[r*size+c]=blocks.at(-1);}
put(4,5,[1,2]);put(5,5,[1,2]);put(6,5,[1,2,4]);
const world={id:'asymmetric',name:'Example',gridSize:size,tiles,blockStacks,characterColumn:4,characterRow:5,characterFacing:'E',house:{column:2,row:9,scale:1.2,visible:true,facing:'W',opacity:35},orangeTrees:[{column:8,row:11,kind:'conifer',size:1,facing:'E'},{column:10,row:3,facing:'N',opacity:25}],fences:[{column:1,row:15,edge:'S',material:'wood'}],bridges:[{column:3,row:16,axis:'E',material:'stone',length:4},{column:9,row:11,axis:'S',material:'wood'}],ramps:[{column:5,row:5,direction:'E',material:4}],blockOpacity:[{column:6,row:5,level:2,opacity:45}]};
const original=structuredClone(world),start={column:4,row:5},end={column:6,row:5};
assert.equal(path.find(world,start,end).length,3);
for(const q of [0,1,2,3,-1,5]){
 const projected=view.projectWorld(world,q);assert(projected);assert.deepEqual(world,original,'no world data changes');
 assert.deepEqual(view.projectWorld(projected,-q),original,'entire snapshot reverses');
 const a=view.point(start.column,start.row,size,q),b=view.point(end.column,end.row,size,q);
 const route=path.find(projected,a,b);assert.equal(route.length,3,'rotated ramp traversal remains valid');
 assert.deepEqual(route.map(p=>view.point(p.column,p.row,size,-q)),[start,{column:5,row:5},end]);
 const w=projected.blockStacks[view.point(6,5,size,q).row*size+view.point(6,5,size,q).column];w.push(10);projected.ramps[0].material=1;assert.deepEqual(world,original,'projection is not aliased to storage');
}
assert.equal(view.projectWorld({...world,ramps:[{column:5,row:5,direction:'invalid'}]},1),null);
assert.equal(view.projectWorld({...world,tiles:[]},1),null);
console.log('Non-mutating four-way terrain/object projection and ramp routing round-trip: OK');
