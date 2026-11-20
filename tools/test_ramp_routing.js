const assert=require('node:assert/strict'),path=require('../preview/gameplay-path.js');
const opposite={N:'S',E:'W',S:'N',W:'E'},delta={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]},size=20;
for(const direction of Object.keys(delta)){
 const [dx,dy]=delta[direction],ramp={column:10,row:10,direction},low={column:10-dx,row:10-dy},high={column:10+dx,row:10+dy},side={column:10+dy,row:10-dx};
 const stacks=Array.from({length:size*size},()=>[]),put=(p,h,material=1)=>{stacks[p.row*size+p.column]=Array(h).fill(material)};
 put(low,2);put(ramp,2);put(high,3);put(side,2);
 const world={gridSize:size,tiles:stacks.map(s=>s.at(-1)||0),blockStacks:stacks,ramps:[ramp]};
 const route=(a,b)=>path.find(world,a,b);
 assert.deepEqual(route(low,high),[low,{column:10,row:10},high]);assert.deepEqual(route(high,low),[high,{column:10,row:10},low]);
 assert.equal(route(ramp,side),null);assert.equal(route(side,ramp),null);
 put(high,4);world.tiles=stacks.map(s=>s.at(-1)||0);assert.equal(route(low,high),null);
 put(high,3);world.tiles=stacks.map(s=>s.at(-1)||0);world.fences=[{column:10,row:10,edge:direction}];assert.equal(route(low,high),null);
 world.fences=[];world.ramps.push({...ramp,direction:opposite[direction]});assert.equal(route(low,high),null);
}
console.log('Ramp routing: both directions, side rejection, cliffs, fences and malformed ramps OK');
