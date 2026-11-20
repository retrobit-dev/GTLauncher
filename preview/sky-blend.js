// Continuous, bounded solar-elevation weights shared by sky and island lighting.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.SkyBlend=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function smooth(low,high,n){const t=Math.max(0,Math.min(1,(n-low)/(high-low)));return t*t*(3-2*t);}
  function weights(altitude){const rise=smooth(-18,-6,altitude),day=smooth(-2,12,altitude);return {night:1-rise,twilight:rise*(1-day),day:day};}
  return Object.freeze({weights});
});
