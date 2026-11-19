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
    // The vertical high end is needed when it is not joined to an upper block.
    const [a,b]=ends;
    const highFace=[vertex(a[2],a[3],0),vertex(b[2],b[3],0),vertex(b[2],b[3],1),vertex(a[2],a[3],1)];
    return {topFace,sideFaces,highFace};
  }
  // A ramp occupies one tile above its supporting stack. Its high edge is one
  // level above its low edge. Side edges are deliberately not traversable.
  // Ordinary flat-to-flat steps keep their existing one-level jump rule.
  function canConnect(a,b,edge){
    if(!DIRECTIONS.includes(edge) || !a || !b || !Number.isInteger(a.height) || !Number.isInteger(b.height) || a.height<1 || b.height<1 ||
       (a.direction != null && !DIRECTIONS.includes(a.direction)) || (b.direction != null && !DIRECTIONS.includes(b.direction)))return false;
    const opposite={N:'S',E:'W',S:'N',W:'E'};
    function endpoint(surface,side){
      if(surface.direction==null)return surface.height;
      if(side===surface.direction)return surface.height+1;
      if(side===opposite[surface.direction])return surface.height;
      return null;
    }
    if(a.direction==null && b.direction==null)return Math.abs(a.height-b.height)<=1;
    const x=endpoint(a,edge),y=endpoint(b,opposite[edge]);
    return x!==null && y!==null && x===y;
  }
  // Height under the moving character, sampled on the actual tile surface.
  // The tile boundary lies halfway between two tile centres.
  function stepHeight(a,b,edge,progress){
    if(!Number.isFinite(progress) || !canConnect(a,b,edge))return null;
    const t=Math.max(0,Math.min(1,progress)),delta={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[edge];
    if(a.direction==null && b.direction==null)return a.height*(1-t)+b.height*t;
    const first=t<.5,surface=first?a:b,offset=first?t:t-1;
    return surface.height+(surface.direction==null?0:fraction(surface.direction,.5+delta[0]*offset,.5+delta[1]*offset));
  }
  return Object.freeze({DIRECTIONS,fraction,create,canConnect,stepHeight});
});
