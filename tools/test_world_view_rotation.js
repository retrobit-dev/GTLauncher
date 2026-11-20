const assert=require('node:assert/strict'),r=require('../preview/world-view-rotation.js');
for(const size of [20,25,50,100])for(const q of [-5,-1,0,1,2,3,4,7]){
 for(const p of [{column:0,row:0},{column:size-1,row:size-1},{column:3,row:7},{column:3.25,row:7.5}])assert.deepEqual(r.point(...Object.values(r.point(p.column,p.row,size,q)),size,-q),p);
 for(const d of ['N','E','S','W'])assert.equal(r.direction(r.direction(d,q),-q),d);
 for(const rect of [{column:1,row:3,width:3,height:4},{column:0,row:0,width:3,height:3}]){const t=r.rectangle(rect.column,rect.row,rect.width,rect.height,size,q);assert.deepEqual(r.rectangle(t.column,t.row,t.width,t.height,size,-q),rect);}
 for(const edge of ['N','E','S','W']){const f={column:2,row:4,edge},t=r.fence(f,size,q);assert.deepEqual(r.fence(t,size,-q),f);}
 for(const axis of ['E','S'])for(const length of [3,4,5]) {const b={column:3,row:4,axis,length},t=r.bridge(b,size,q);assert.deepEqual(r.bridge(t,size,-q),b);}
}
assert.deepEqual(r.point(3,7,20,1),{column:12,row:3});assert.equal(r.direction('N',1),'E');assert.deepEqual(r.rectangle(2,3,3,4,20,1),{column:13,row:2,width:4,height:3});
assert.equal(r.point(0,0,0,1),null);assert.equal(r.direction('X',1),null);assert.equal(r.rectangle(19,19,3,3,20,1),null);assert.equal(r.bridge({column:19,row:0,axis:'E'},20,1),null);
console.log('Quarter-turn view coordinates, facing, footprints, edges and bridges round-trip: OK');
