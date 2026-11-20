const assert=require('node:assert/strict'),rot=require('../preview/world-view-rotation.js'),ramp=require('../preview/ramp-geometry.js');
const size=20,stacks=Array.from({length:size*size},()=>[]);stacks[8*size+6]=[1,2,4];const world={gridSize:size,tiles:stacks.map(a=>a.at(-1)||0),blockStacks:stacks,ramps:[{column:6,row:8,direction:'E',material:5}]};
const saved=structuredClone(world),ox=200,oy=100,width=28,depth=14,block=4,base=20;
for(const q of [0,1,2,3,-1]) {
 const p=rot.point(6,8,size,q),cx=ox+(p.column-p.row)*width/2,cy=oy+(p.column+p.row)*depth/2-8;
 const shape=ramp.create(rot.direction('E',q),width,depth,block);
 const visible=[shape.topFace,...shape.sideFaces.filter(f=>f.every(p=>p.u===1)||f.every(p=>p.v===1)),...(['E','S'].includes(rot.direction('E',q))?[shape.highFace]:[])];
 for(const face of visible){const x=cx+face.reduce((n,v)=>n+v.x,0)/face.length,y=cy+face.reduce((n,v)=>n+v.y,0)/face.length;assert.deepEqual(rot.pickWorld(world,q,x,y,ox,oy,width,depth,block,base),{column:6,row:8});}
 assert.equal(rot.pickWorld(world,q,-999,-999,ox,oy,width,depth,block,base),null);
 const view=rot.projectWorld(world,q);
 for(let x=cx-18;x<=cx+18;x+=3)for(let y=cy-15;y<=cy+26;y+=3){const fromWorld=rot.pickWorld(world,q,x,y,ox,oy,width,depth,block,base),fromView=rot.pickWorld(view,0,x,y,ox,oy,width,depth,block,base);assert.deepEqual(fromWorld,fromView?rot.point(fromView.column,fromView.row,size,-q):null);}
}
assert.deepEqual(world,saved);console.log('Four-way ramp/top/wall picking maps screen positions back to unmodified world coordinates: OK');
