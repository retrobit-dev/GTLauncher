const assert=require('node:assert/strict'),g=require('../preview/ramp-geometry.js');
const near=(a,b)=>assert(Math.abs(a-b)<1e-9,`${a} != ${b}`);
for(const direction of g.DIRECTIONS)for(const width of [20,28])for(const depth of [8,14])for(const height of [4,9]) {
 const shape=g.create(direction,width,depth,height);assert.equal(shape.topFace.length,4);assert.equal(shape.sideFaces.length,2);
 for(const face of [shape.topFace,...shape.sideFaces.filter(face=>face.every(p=>p.u===1)||face.every(p=>p.v===1)),...(['E','S'].includes(direction)?[shape.highFace]:[])]) {
  const x=face.reduce((n,p)=>n+p.x,0)/face.length,y=face.reduce((n,p)=>n+p.y,0)/face.length;
  assert(g.hit(direction,width,depth,height,x,y));
  for(const p of face)assert(g.hit(direction,width,depth,height,p.x,p.y));
 }
 assert(!g.hit(direction,width,depth,height,10000,10000));
 for(const p of shape.topFace){near(p.z,g.fraction(direction,p.u,p.v));near(p.x,(p.u-p.v)*width/2);near(p.y,(p.u+p.v-1)*depth/2-p.z*height);}
 for(const [a,b,c] of shape.sideFaces){near(a.z,0);near(b.z,0);near(c.z,1);near(b.u,c.u);near(b.v,c.v);near(Math.abs(a.u-b.u)+Math.abs(a.v-b.v),1);near(g.fraction(direction,a.u,a.v),0);near(g.fraction(direction,c.u,c.v),1);}
 const [a,b,c,d]=shape.highFace;near(a.z,0);near(b.z,0);near(c.z,1);near(d.z,1);near(a.u,d.u);near(a.v,d.v);near(b.u,c.u);near(b.v,c.v);near(Math.abs(a.u-b.u)+Math.abs(a.v-b.v),1);near(g.fraction(direction,a.u,a.v),1);near(g.fraction(direction,b.u,b.v),1);
 for(let i=0;i<=10;i++) {const t=i/10; const uv=direction==='E'?[t,.5]:direction==='W'?[1-t,.5]:direction==='S'?[.5,t]:[.5,1-t];near(g.fraction(direction,...uv),t);}
}
assert.equal(g.create('X',28,14,4),null);assert.equal(g.create('E',28,14,0),null);assert.equal(g.fraction('E',1.1,0),null);
const opposite={N:'S',E:'W',S:'N',W:'E'};
for(const dir of g.DIRECTIONS){
 const ramp={height:2,direction:dir}, low={height:2},high={height:3};
 for(const t of [0,.25,.5,.75,1]){
  near(g.stepHeight(low,ramp,dir,t),2+Math.max(0,t-.5));
  near(g.stepHeight(ramp,high,dir,t),2.5+Math.min(t,.5));
  near(g.stepHeight(high,ramp,opposite[dir],1-t),g.stepHeight(ramp,high,dir,t));
 }
 assert(g.canConnect(ramp,high,dir));assert(g.canConnect(high,ramp,opposite[dir]));
 assert(g.canConnect(ramp,low,opposite[dir]));assert(g.canConnect(low,ramp,dir));
 assert(!g.canConnect(ramp,{height:4},dir));assert(!g.canConnect(ramp,{height:1},opposite[dir]));
 for(const side of g.DIRECTIONS.filter(x=>x!==dir&&x!==opposite[dir]))assert(!g.canConnect(ramp,low,side));
 assert(g.canConnect(ramp,{height:3,direction:dir},dir));assert(!g.canConnect(ramp,{height:2,direction:dir},dir));
}
assert(g.canConnect({height:2},{height:3},'E'));assert(!g.canConnect({height:2},{height:4},'E'));assert(!g.canConnect({height:0},{height:1},'E'));assert(!g.canConnect({height:2,direction:'bad'},{height:2},'E'));
console.log('Ramp wedge geometry: four directions, planar incline, exact triangular sides and endpoints OK');
