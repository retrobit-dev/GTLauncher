// The visual island-centred orbit and cast-shadow proportions match Android CelestialArc.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.CelestialGeometry=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function offset(hourAngle,declination,latitude){
    const r=Math.PI/180,cosH=-Math.tan(latitude*r)*Math.tan(declination*r);
    const horizon=Number.isFinite(cosH)&&Math.abs(cosH)<1?Math.acos(cosH)/r:180;
    const progress=Math.max(0,Math.min(1,.5+hourAngle/(2*horizon))),angle=progress*Math.PI;
    return {x:300*Math.cos(angle),y:-175*Math.sin(angle)};
  }
  function shadowScale(hourAngle,declination,latitude,depth){const p=offset(hourAngle,declination,latitude),distance=Math.hypot(p.x,p.y);return distance/(distance-depth);}
  function shadow(bottomY,tileHeight,halfHeight){return bottomY-tileHeight/2+50-halfHeight;}
  return Object.freeze({offset,shadowScale,shadow});
});
