// Built-in Base World template. Layers are ordered bottom to top; new worlds
// receive independent copies, and existing saved worlds are never migrated.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.BaseTerrain=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const SIZE=50, LAYERS=Object.freeze([2,2,2,2,1]);
  function stack(column,row){return Number.isInteger(column)&&Number.isInteger(row)&&column>=0&&row>=0&&column<SIZE&&row<SIZE?LAYERS.slice():null;}
  return Object.freeze({SIZE,LAYERS,stack});
});
