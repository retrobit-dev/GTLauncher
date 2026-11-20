const assert=require('node:assert/strict');const {offset}=require('../preview/fence-shadow.js');
for(const b of [{x:16,y:8},{x:16,y:-8}])for(const light of [{x:-500,y:200},{x:500,y:0},{x:8,y:-300}]){
 const a={x:0,y:0},d=offset(a,b,light,175);assert(Math.abs(d.x)<=Math.abs(b.x-a.x)*.14+1e-9);assert(Math.abs(d.y)+Math.abs(b.y-a.y)<=25+1e-9);
}
assert(Math.abs(offset({x:0,y:0},{x:16,y:8},{x:8,y:-500},175,9).y)<=9);
assert(offset({x:0,y:0},{x:16,y:8},{x:-500,y:0},175).x>0);
assert(offset({x:0,y:0},{x:16,y:8},{x:500,y:0},175).x<0);
console.log('Fence shadow contact, direction and bounded projection: OK');
