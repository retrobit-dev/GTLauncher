const assert=require('node:assert/strict'),road=require('../preview/road-surface.js');
assert.deepEqual(road.materials,['dirt','stone','sand','paving','asphalt','water']);
assert.equal(road.isWater('water'),true);for(const m of road.materials.filter(m=>m!=='water'))assert.equal(road.isWater(m),false);
assert.deepEqual(road.outline({N:true,E:true}),[[.1,0],[1,0],[1,.9],[.1,.9]]);
const a={column:2,row:2,material:'water'},b={column:3,row:2,material:'water'},roads=[a,b];
assert(road.validRecords(roads,20));assert(!road.validRecords([...roads,a],20));assert(!road.validRecords([{column:20,row:2,material:'dirt'}],20));
assert.deepEqual(road.neighbors(roads,2,2,'water'),{N:false,E:true,S:false,W:false});assert.equal(road.put(roads,20,a,()=>true),null);assert.equal(road.put(roads,20,{column:4,row:2,material:'water'},()=>false),null);
assert.equal(road.put(roads,20,{column:4,row:2,material:'water'},()=>true).length,3);assert.equal(roads.length,2);
let calls=[];const context=new Proxy({}, {get(t,k){return t[k]||((...a)=>calls.push([k,...a]));},set(t,k,v){t[k]=v;return true;}});
for(const m of road.materials){calls=[];assert(road.draw(context,{x:50,y:50,width:40,height:20,material:m,column:4,row:8,time:1}));assert(calls.some(a=>a[0]==='fill'));assert(calls.some(a=>a[0]==='clip'));assert(calls.some(a=>a[0]==='restore'));}
assert.equal(road.draw(context,{x:0,y:0,width:1,height:1,material:'unknown'}),false);
console.log('Road materials, river classification, edge connections and isolated surface drawing: OK');
