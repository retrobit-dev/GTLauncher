const assert=require('node:assert/strict'),g=require('../preview/ramp-geometry.js');
const near=(a,b)=>assert(Math.abs(a-b)<1e-9,`${a} != ${b}`);
for(const direction of g.DIRECTIONS)for(const width of [20,28])for(const depth of [8,14])for(const height of [4,9]) {
 const shape=g.create(direction,width,depth,height);assert.equal(shape.topFace.length,4);assert.equal(shape.sideFaces.length,2);
 for(const p of shape.topFace){near(p.z,g.fraction(direction,p.u,p.v));near(p.x,(p.u-p.v)*width/2);near(p.y,(p.u+p.v-1)*depth/2-p.z*height);}
 for(const [a,b,c] of shape.sideFaces){near(a.z,0);near(b.z,0);near(c.z,1);near(b.u,c.u);near(b.v,c.v);near(Math.abs(a.u-b.u)+Math.abs(a.v-b.v),1);near(g.fraction(direction,a.u,a.v),0);near(g.fraction(direction,c.u,c.v),1);}
 for(let i=0;i<=10;i++) {const t=i/10; const uv=direction==='E'?[t,.5]:direction==='W'?[1-t,.5]:direction==='S'?[.5,t]:[.5,1-t];near(g.fraction(direction,...uv),t);}
}
assert.equal(g.create('X',28,14,4),null);assert.equal(g.create('E',28,14,0),null);assert.equal(g.fraction('E',1.1,0),null);
console.log('Ramp wedge geometry: four directions, planar incline, exact triangular sides and endpoints OK');
