// Bounded ground projection for one fence edge; both endpoints remain in contact.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.FenceShadow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function offset(a,b,light,lift) {
    const mx=(a.x+b.x)/2,my=(a.y+b.y)/2,span=Math.abs(b.x-a.x);
    const clamp=(n,max)=>Math.max(-max,Math.min(max,n));
    const dx=clamp((mx-light.x)*.08,span*.30);
    let dy=clamp((my-light.y-lift)*.08,Math.max(0,Math.min(12,25-Math.abs(b.y-a.y))));
    if(Math.abs(dy)<.75)dy=dy<0?-.75:.75;
    return {x:dx,y:dy};
  }
  return Object.freeze({offset});
});
