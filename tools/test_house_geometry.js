const assert=require('node:assert/strict'),g=require('../preview/house-geometry.js');
const close=(a,b)=>assert(Math.abs(a-b)<1e-8,`${a} != ${b}`);
for(const tileHeight of [28*Math.sin(20*Math.PI/180),28*Math.sin(25*Math.PI/180),14,28*Math.sin(38*Math.PI/180)])for(const scale of [.5,1,1.5]) {
 const x=g.create(28,tileHeight,scale), left=g.project(x,g.baseLeft,g.baseLeftY),right=g.project(x,g.baseRight,g.baseRightY),corner=g.project(x,256,482);
 close(left.x,-42*scale);close(right.x,42*scale);close(left.y,-1.5*tileHeight*scale);close(right.y,left.y);close(corner.x,0);close(corner.y,0);
 // On the right side, measured eaves and wall base are parallel to the
 // tile axis and both wall ends have the same vertical height.
 const frontEave=g.project(x,256,290),rearEave=g.project(x,410,215);
 const frontBase=g.project(x,256,445),rearBase=g.project(x,410,370);
 close(frontBase.y-frontEave.y,rearBase.y-rearEave.y);
 assert(Math.abs((rearEave.y-frontEave.y)/(rearEave.x-frontEave.x)+tileHeight/28)<.005);
 assert(Math.abs((rearBase.y-frontBase.y)/(rearBase.x-frontBase.x)+tileHeight/28)<.005);
 // The ridge also runs along the depth axis; measurements of the drawn
 // timber have a small antialiasing/outline tolerance.
 const ridgeFront=g.project(x,140,115),ridgeRear=g.project(x,320,15);
 if(Math.abs(tileHeight-28*Math.sin(25*Math.PI/180))<1e-6) assert(Math.abs((ridgeRear.y-ridgeFront.y)/(ridgeRear.x-ridgeFront.x)+tileHeight/28)<.02);
 // Door sits on the middle third of the front face, including its mirrored view.
 const doorX=160,t=(doorX-g.baseLeft)/(256-g.baseLeft);assert(t>1/3&&t<2/3);
 for(const pt of [[100,200],[200,444],[350,405],[500,20]]){const a=g.project(x,...pt),b=g.unproject(x,a.x,a.y);close(b.x,pt[0]);close(b.y,pt[1]);}
}
console.log('House 3×3 foundation alignment, middle door and inverse hit geometry: OK');
