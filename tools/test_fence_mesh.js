const assert=require('node:assert/strict');const {faces,contains}=require('../preview/fence-mesh.js');
for(const b of [{x:14,y:7},{x:14,y:-7}]) {
 const a={x:0,y:0},wood=faces(a,b,'wood'),stone=faces(a,b,'stone');
 assert.equal(wood.length,14);assert.equal(stone.length,28);
 for(const material of [wood,stone])for(const face of material) {
  assert.equal(face.points.length,4);assert(face.points.every(p=>Number.isFinite(p.x)&&Number.isFinite(p.y)));
  const centre=face.points.reduce((p,v)=>({x:p.x+v.x/4,y:p.y+v.y/4}),{x:0,y:0});assert(contains(face.points,centre.x,centre.y));
 }
 const woodTop=Math.min(...wood.flatMap(f=>f.points.map(p=>p.y))),stoneTop=Math.min(...stone.flatMap(f=>f.points.map(p=>p.y)));
 assert(stoneTop>woodTop,'stone wall is smaller than the wooden posts');
 assert(!stone.some(f=>contains(f.points,1000,1000)),'empty space is not selectable');
}
console.log('Fence solid faces, depth, reduced stone height and hit testing: OK');
const tall=faces({x:0,y:0},{x:14,y:7},'stone-tall');
const highest=Math.min(...tall.flatMap(f=>f.points.map(p=>p.y)));
assert(Math.abs(highest - (-127.5+.1))<1,'wall height is 1.5 times the default 85-unit house');
assert(tall.length>200,'tall wall is built from independent stone courses');
console.log('Tall stone wall height and masonry: OK');
