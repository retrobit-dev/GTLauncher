// Built-in Base World template. Layers are ordered bottom to top; new worlds
// receive independent copies, and existing saved worlds are never migrated.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.BaseTerrain=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const SIZE=50, LAYERS=Object.freeze([2,2,2,2,1]);
  function stack(column,row){return Number.isInteger(column)&&Number.isInteger(row)&&column>=0&&row>=0&&column<SIZE&&row<SIZE?LAYERS.slice():null;}
  // Legacy stacks have no provenance flag: level zero is their base. Recognize
  // four Base-template soil courses when intact; higher additions stay blocks.
  function isBaseLayer(layers,level){if(!Array.isArray(layers) || !Number.isInteger(level) || level<0 || level>=layers.length)return false;if(level===0)return true;return layers.length>=4 && layers.slice(0,4).every(v=>v===2) && (level<4 || (level===4 && layers[4]===1));}
  return Object.freeze({SIZE,LAYERS,stack,isBaseLayer});
});
