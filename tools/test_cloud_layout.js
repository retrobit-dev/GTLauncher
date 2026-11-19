const assert=require('node:assert/strict'),c=require('../preview/cloud-layout');
for(let cover=0;cover<=100;cover++) {
  for(const code of [0,1])assert(c.count(cover,code)<=3 && c.count(cover,code)>=1);
  for(const code of [2,3,45,48,51,63,80])assert(c.count(cover,code)<=6);
  for(const code of [95,96,99])assert(c.count(cover,code)>=7 && c.count(cover,code)<=10);
}
assert.equal(c.count(0,95),7);assert.equal(c.count(100,95),10);
for(const grid of [9,20,25,50,100])for(const count of [1,3,6,10]) {
 const cell=c.cellSize(28,14,28,14,grid,count);assert(cell>=6);if(grid===50 && count===3)assert(cell>20);
 const positions=Array.from({length:count},(_,i)=>c.seed(i,count,grid,.5,.5));
 for(const p of positions)assert(p.east>=0 && p.east<grid && p.north>=0 && p.north<grid);
 if(count>=3){assert(positions.some(p=>p.east<grid*.25));assert(positions.some(p=>p.east>grid*.75));assert(positions.some(p=>p.north<grid*.25));assert(positions.some(p=>p.north>grid*.70));}
}
assert.equal(c.fade(0,true),1);assert.ok(Math.abs(c.fade(12,true)-.6)<1e-10);assert.equal(c.fade(12,false),1);
assert.equal(c.wet(20,63),true);assert.equal(c.wet(5,0),false);
console.log('Weather limits, grid-wide cloud seeds, enlarged shadows and reduced-motion fade: OK');
