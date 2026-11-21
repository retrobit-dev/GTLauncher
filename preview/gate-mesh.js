// Isometric gate: stone or wood posts and a hinged timber leaf.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.GateMesh=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
 'use strict';
 function faces(a,b,edge,material,amount=0,side=1) {
  const dx=b.x-a.x,dy=b.y-a.y,vertical=edge==='E'||edge==='W',sign=side<0?-1:1;
  const normal=vertical?{x:-sign*Math.abs(dx),y:-sign*Math.abs(dy)}:{x:sign*Math.abs(dx),y:-sign*Math.abs(dy)};
  const angle=Math.max(0,Math.min(1,amount))*Math.PI/2, end={x:a.x+dx*Math.cos(angle)+normal.x*Math.sin(angle),y:a.y+dy*Math.cos(angle)+normal.y*Math.sin(angle)};
  const list=[],polygon=(points,color)=>list.push({points,color});
  function post(p,stone){const w=stone?2:1.6,h=stone?13:15;polygon([{x:p.x-w,y:p.y},{x:p.x+w,y:p.y},{x:p.x+w,y:p.y-h},{x:p.x-w,y:p.y-h}],stone?'#909a88':'#a67c56');polygon([{x:p.x-w,y:p.y-h},{x:p.x,y:p.y-h-1.4},{x:p.x+w,y:p.y-h},{x:p.x,y:p.y-h+1.4}],stone?'#c4c7b8':'#d0ab79');}
  post(a,material==='gate-stone');post(b,material==='gate-stone');
  const p=(u,z)=>({x:a.x+(end.x-a.x)*u,y:a.y+(end.y-a.y)*u-z});
  for(let i=0;i<5;i++){let u=.08+i*.17;polygon([p(u,1),p(u+.13,1),p(u+.13,10),p(u,10)],i%2?'#a6784f':'#b78b60');}
  for(const z of [3,8])polygon([p(.06,z),p(.94,z),p(.94,z+1.5),p(.06,z+1.5)],'#80583b');
  return list;
 }
 return Object.freeze({faces});
});
