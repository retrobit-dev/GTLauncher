const assert=require('node:assert/strict'),path=require('../preview/gameplay-path');
const size=20,world={gridSize:size,tiles:Array(size*size).fill(1),roads:[]},p=(column,row)=>({column,row});
for(let c=2;c<=10;c++)world.roads.push({column:c,row:3,material:'dirt'});
let route=path.find(world,p(2,2),p(10,2),null);assert.equal(route.length,11);assert(route.some(t=>t.row===3),'modest road detour');assert.equal(path.score(world,route),31);
route=path.find(world,p(2,2),p(6,2),null);assert.equal(route.length,5);assert(route.every(t=>t.row===2),'avoid excessive detour');
const dry={...world,roads:world.roads.map(r=>({...r,material:'water'}))};route=path.find(dry,p(2,2),p(10,2),null);assert.equal(route.length,9);assert(route.every(t=>t.row===2),'river is not a preferred path or traversable');
world.fences=[{column:5,row:3,edge:'E',material:'wood'}];route=path.find(world,p(2,2),p(10,2),null);assert(!route.some((t,i)=>i>0 && ((t.column===6&&t.row===3&&route[i-1].column===5&&route[i-1].row===3)||(t.column===5&&t.row===3&&route[i-1].column===6&&route[i-1].row===3))),'ordinary fence blocks route');
const wet={gridSize:size,tiles:Array(size*size).fill(0),roads:[{column:6,row:5,material:'dirt'}]};for(const [x,y,v] of [[5,5,3],[5,6,1],[6,5,3],[7,5,1]])wet.tiles[y*size+x]=v;
assert.deepEqual(path.findNearestDry(wet,p(5,5),null),[p(5,5),p(5,6)],'water recovery remains shortest');
const equal={gridSize:size,tiles:Array(size*size).fill(1),roads:[{column:3,row:2,material:'stone'},{column:3,row:3,material:'stone'}]};assert(path.find(equal,p(2,2),p(3,3),null).some(t=>t.column===3&&t.row===2),'equal length prefers road');
// Compare against an independent breadth-first search on varied obstacle layouts.
let seed=7;const rand=()=>((seed=(seed*1664525+1013904223)>>>0)/4294967296);
for(let trial=0;trial<50;trial++){
 const t=Array.from({length:400},()=>rand()<.2?0:1);t[21]=1;t[378]=1;
 const roads=t.flatMap((v,i)=>v && rand()<.4?[{column:i%20,row:Math.floor(i/20),material:'paving'}]:[]);
 const w={gridSize:20,tiles:t,roads},start=p(1,1),end=p(18,18),queue=[[start,0]],seen=new Set([21]);let shortest=null;
 for(let k=0;k<queue.length;k++){const [v,n]=queue[k];if(v.column===18&&v.row===18){shortest=n;break;}for(const [dx,dy] of [[0,-1],[1,0],[0,1],[-1,0]]){const c=v.column+dx,r=v.row+dy,key=r*20+c;if(c>=0&&c<20&&r>=0&&r<20&&t[key]&&!seen.has(key)){seen.add(key);queue.push([p(c,r),n+1]);}}}
 const result=path.find(w,start,end,null);if(shortest===null)assert.equal(result,null);else {assert(result);assert((result.length-1)*3<=shortest*4,'route detour is bounded');for(const tile of result)assert(t[tile.row*20+tile.column]);}
}
console.log('Road preference, bounded detours, obstacles, water recovery: OK');
