const assert=require('node:assert/strict'),{projection}=require('../preview/house-shadow');
for(const s of [.5,1,1.5])for(let a=0;a<360;a+=5)for(let e=0;e<=90;e+=5){
 const w=84*s,h=85*s,p=projection(w,h,a,e);
 assert(Number.isFinite(p.dx)&&Number.isFinite(p.dy)&&p.scale>0&&p.scale<=1);
 assert((w/2+Math.abs(p.dx))*p.scale<=w/2+1e-8);
 assert(Math.abs(p.dy)*p.scale<=h/2+1e-8);
}
const n=projection(84,85,0,30),s=projection(84,85,180,30);assert(n.dx<0 && s.dx>0);assert(n.dy>0 && s.dy<0);
assert(Math.abs(projection(84,85,0,80).dx)<Math.abs(n.dx));
console.log('House silhouette projection follows light and stays within house dimensions: OK');
