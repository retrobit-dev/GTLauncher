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
    assert.ok((62+Math.abs(p.dx+sway))*p.scale<=62+1e-9,'shadow must fit tree width');
    assert.ok(Math.abs(p.dy)*p.scale<=height+1e-9,'shadow must fit tree height');
  }
}
for(const scale of [.5,1,1.5]) for(let x=-1000;x<=1000;x+=100) for(let y=-1000;y<=1000;y+=100) {
  const width=62*scale,h=60*scale,sway=5*scale,p=projection(anchor,{x,y},width,h,175,sway);
  assert((width+Math.abs(p.dx+sway))*p.scale<=width+1e-8);
  assert(Math.abs(p.dy)*p.scale<=h+1e-8);
  assert(p.scale>0 && p.scale<=1);
}
assert.ok(projection(anchor,{x:-100,y:0},62,height,100,0).dx>0);
assert.ok(projection(anchor,{x:300,y:0},62,height,100,0).dx<0);
console.log('Tree shadow anchor, light direction, sway and projection bounds: OK');
