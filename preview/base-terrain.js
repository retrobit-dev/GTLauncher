// Built-in Base World template. Layers are ordered bottom to top; new worlds
// receive independent copies, and existing saved worlds are never migrated.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.BaseTerrain=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const SIZE=50, LAYERS=Object.freeze([2,2,2,2,1]);
  function stack(column,row){return Number.isInteger(column)&&Number.isInteger(row)&&column>=0&&row>=0&&column<SIZE&&row<SIZE?LAYERS.slice():null;}
  // The saved format has no provenance flag. Only recognize the intact four
  // soil-layer template prefix; all other layers keep normal block styling.
  function isBaseLayer(layers,level){return Array.isArray(layers) && layers.length>=4 && layers.slice(0,4).every(v=>v===2) && Number.isInteger(level) && level>=0 && (level<4 || (level===4 && layers[4]===1));}
  return Object.freeze({SIZE,LAYERS,stack,isBaseLayer});
});
