// Bounded ground projection of the actual house silhouette, sharing light direction with player shadows.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.HouseShadow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function projection(width,height,azimuth,altitude) {
    const a=azimuth*Math.PI/180,e=Math.max(8,Math.min(90,altitude))*Math.PI/180;
    const distance=Math.min(height*3,height/Math.tan(e));
    const dx=-distance*(Math.sin(a)+Math.cos(a))/Math.SQRT2;
    let dy=-distance*(Math.sin(a)-Math.cos(a))*.30;
    if(Math.abs(dy)<height*.12)dy=(dy<0?-1:1)*height*.12;
    const scale=Math.min(1,width/(width+2*Math.abs(dx)),height/(2*Math.abs(dy)));
    return {dx,dy,scale};
  }
  return Object.freeze({projection});
});
