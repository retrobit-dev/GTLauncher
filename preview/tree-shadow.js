// Project a 2D tree alpha silhouette onto the ground from its rooted anchor.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.TreeShadow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function projection(anchor,light,width,height,lift,sway=0) {
    const h=Math.min(Math.max(height,0),lift*.8), ratio=h/(lift-h);
    const clamp=(v,lo,hi)=>Math.max(lo,Math.min(hi,v));
    const dx=clamp((anchor.x-light.x)*ratio,-height*1.5,height*1.5);
    let dy=clamp((anchor.y-light.y-lift)*ratio,-height*.9,height*.9);
    if(Math.abs(dy)<height*.2)dy=(dy<0?-1:1)*height*.2;
    // Shrink uniformly around the root so the complete projected rectangle fits
    // within the sprite's width and height even at low sun angles and high sway.
    const scale=Math.min(1,width/(width+Math.abs(dx+sway)),height/Math.abs(dy));
    return {dx,dy,shear:-(dx+sway)/height,vertical:-dy/height,scale};
  }
  return Object.freeze({projection});
});
