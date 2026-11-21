const assert=require('node:assert/strict'),mesh=require('../preview/gate-mesh.js');
for(const [edge,a,b] of [['N',{x:0,y:-6},{x:14,y:0}],['E',{x:0,y:6},{x:14,y:0}],['S',{x:-14,y:0},{x:0,y:6}],['W',{x:-14,y:0},{x:0,y:-6}]])for(const material of ['gate-wood','gate-stone']){
 const closed=mesh.faces(a,b,edge,material,0,1),negative=mesh.faces(a,b,edge,material,1,-1),positive=mesh.faces(a,b,edge,material,1,1);
 assert.equal(closed.length,11);assert.deepEqual(closed.slice(0,4),negative.slice(0,4),'posts stay fixed');
 const dxNeg=negative[4].points[1].x-a.x,dyNeg=negative[4].points[1].y-a.y+1,dxPos=positive[4].points[1].x-a.x,dyPos=positive[4].points[1].y-a.y+1;
 assert(Math.abs(dxNeg+dxPos)<1e-8 && Math.abs(dyNeg+dyPos)<1e-8,'opposite approaches swing opposite ways');
 const vertical=edge==='E'||edge==='W';assert.equal(Math.sign(dxNeg),vertical?1:-1);assert(dyNeg>0);
}
console.log('Gate leaf, fixed posts and outward swing for all edges: OK');
