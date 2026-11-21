const assert=require('node:assert/strict'),road=require('../preview/road-surface.js');
assert.deepEqual(road.materials,['dirt','stone','sand','paving','asphalt','water']);
assert.equal(road.isWater('water'),true);for(const m of road.materials.filter(m=>m!=='water'))assert.equal(road.isWater(m),false);
assert.equal(road.depth('dirt',4),0);assert.equal(road.depth('water',4),0);assert.equal(road.bedDepth('water',4),0);for(const m of ['stone','sand','paving','asphalt'])assert.equal(road.depth(m,4),0);assert.equal(road.depth('unknown',4),0);
for(const [direction,isEdge] of [['W',p=>p[0]===0],['E',p=>p[0]===1],['N',p=>p[1]===0],['S',p=>p[1]===1]]){const connected={[direction]:true};assert(!road.banks(connected).some(([a,b])=>isEdge(a)&&isEdge(b)),`shared ${direction} must have no bank`);}
assert.deepEqual(road.sharedSpan({E:true},{W:true},'E'),[[.1,.9]]);
assert.deepEqual(road.sharedSpan({S:true},{N:true},'S'),[[.1,.9]]);
assert.deepEqual(road.sharedSpan({E:true,N:true,NE:true},{W:true,N:true,NW:true},'E'),[[0,.9]]);
assert.deepEqual(road.sharedSpan({E:true},{},'E'),[]);
const corner=road.outline({N:true,E:true});assert.deepEqual(corner,[[.1,0],[.9,0],[.9,.1],[1,.1],[1,.9],[.1,.9]]);
assert.deepEqual(road.outline({N:true,E:true,NE:true}),[[.1,0],[1,0],[1,.9],[.1,.9]]);
for(let mask=0;mask<256;mask++){const flags=Object.fromEntries(['N','E','S','W','NE','SE','SW','NW'].map((name,i)=>[name,!!(mask&(1<<i))])),polygon=road.outline(flags);for(let i=0;i<polygon.length;i++){const a=polygon[i],b=polygon[(i+1)%polygon.length];assert(a[0]===b[0] || a[1]===b[1],`junction edge is not perpendicular: ${mask}`);}}
const a={column:2,row:2,material:'water'},b={column:3,row:2,material:'water'},roads=[a,b];
assert(road.validRecords(roads,20));assert(!road.validRecords([...roads,a],20));assert(!road.validRecords([{column:20,row:2,material:'dirt'}],20));
assert.deepEqual(road.neighbors(roads,2,2,'water'),{N:false,E:true,S:false,W:false});assert.equal(road.put(roads,20,a,()=>true),null);assert.equal(road.put(roads,20,{column:4,row:2,material:'water'},()=>false),null);
assert.equal(road.put(roads,20,{column:4,row:2,material:'water'},()=>true).length,3);assert.equal(roads.length,2);
let calls=[];const context=new Proxy({}, {get(t,k){return t[k]||((...a)=>calls.push([k,...a]));},set(t,k,v){t[k]=v;return true;}});
for(const m of road.materials){calls=[];assert(road.draw(context,{x:50,y:50,width:40,height:20,material:m,column:4,row:8,time:1}));assert(calls.some(a=>a[0]==='fill'));assert(calls.some(a=>a[0]==='clip'));assert(calls.some(a=>a[0]==='restore'));assert(!calls.some(a=>a[0]==='translate'),'road surface is flush with terrain');}
for(const d of ['N','E','S','W']){const p=road.flowLines(0,d)[0][0],q=road.flowLines(.5,d)[0][0],delta=[q[0]-p[0],q[1]-p[1]];if(d==='N')assert(delta[1]<0);if(d==='S')assert(delta[1]>0);if(d==='E')assert(delta[0]>0);if(d==='W')assert(delta[0]<0);assert.deepEqual(road.flowLines(0,d),road.flowLines(0,d));}
assert(road.validRecords([{...a,direction:'N'}],20));assert(road.validRecords([{column:0,row:0,material:'dirt',direction:'N'}],20));assert(!road.validRecords([{column:0,row:0,material:'dirt',direction:'bad'}],20));
assert.equal(road.draw(context,{x:0,y:0,width:1,height:1,material:'unknown'}),false);
console.log('Road materials, river classification, edge connections and isolated surface drawing: OK');
