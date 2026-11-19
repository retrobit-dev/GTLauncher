// Visual arc only. Routing still rejects steps more than one block high.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.JumpMotion=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function height(from,to,progress,blockHeight=4){
    if(![from,to,progress,blockHeight].every(Number.isFinite) || from<1 || to<1 || Math.abs(from-to)!==1 || blockHeight<=0)return 0;
    const t=Math.max(0,Math.min(1,progress));
    return Math.sin(Math.PI*t)*blockHeight*.75;
  }
  return Object.freeze({height});
});
