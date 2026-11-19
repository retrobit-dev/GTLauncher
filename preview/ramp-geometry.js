// Geometry only: a single-cell wedge rising exactly one block in a cardinal direction.
// It does not alter terrain storage or movement by itself.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.RampGeometry=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const DIRECTIONS=Object.freeze(['N','E','S','W']);
  function fraction(direction,u,v){
    if(!DIRECTIONS.includes(direction) || !Number.isFinite(u) || !Number.isFinite(v) || u<0 || u>1 || v<0 || v>1)return null;
    return direction==='N'?1-v:direction==='E'?u:direction==='S'?v:1-u;
  }
  function create(direction,width,depth,height){
    if(!DIRECTIONS.includes(direction) || ![width,depth,height].every(n=>Number.isFinite(n)&&n>0))return null;
    // u/v are tile axes; z is elevation in units of a full block.
    const vertex=(u,v,z)=>({u,v,z,x:(u-v)*width/2,y:(u+v-1)*depth/2-z*height});
    const top=(u,v)=>vertex(u,v,fraction(direction,u,v));
    const topFace=[top(0,0),top(1,0),top(1,1),top(0,1)];
    // Each side parallel to the slope is one right triangle in 3D.
    const ends=direction==='E'?[[0,0,1,0],[0,1,1,1]]:direction==='W'?[[1,0,0,0],[1,1,0,1]]:
      direction==='S'?[[0,0,0,1],[1,0,1,1]]:[[0,1,0,0],[1,1,1,0]];
    const sideFaces=ends.map(([lu,lv,hu,hv])=>[vertex(lu,lv,0),vertex(hu,hv,0),vertex(hu,hv,1)]);
    return {topFace,sideFaces};
  }
  return Object.freeze({DIRECTIONS,fraction,create});
});
