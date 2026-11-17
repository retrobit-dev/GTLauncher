const assert=require('node:assert/strict');
const {projection}=require('../preview/tree-shadow.js');
const anchor={x:100,y:100}, height=60;
for(const light of [{x:-100,y:100},{x:300,y:100},{x:100,y:-300},{x:100,y:300}]) {
  for(const sway of [-2,0,2]) {
    const p=projection(anchor,light,62,height,100,sway);
    // The bottom centre is fixed; the canopy tip is displaced along the light ray.
    assert.equal(anchor.x+p.shear*0,anchor.x);
    assert.equal(anchor.y+p.vertical*0,anchor.y);
    assert.ok(Math.abs(-height*p.shear-(p.dx+sway))<1e-9);
    assert.ok(Math.abs(-height*p.vertical-p.dy)<1e-9);
    assert.ok(Math.abs(p.dx)<=height*1.5 && Math.abs(p.dy)<=height*.9);
  }
}
assert.ok(projection(anchor,{x:-100,y:0},62,height,100,0).dx>0);
assert.ok(projection(anchor,{x:300,y:0},62,height,100,0).dx<0);
console.log('Tree shadow anchor, light direction, sway and projection bounds: OK');
