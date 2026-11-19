// Geometry shared by future tile-edge fence placement and routing.
// An edge has a canonical owner, so E on one tile and W on its neighbour
// describe the same physical boundary. Data is not merged or mutated.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.FenceEdges=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function canonical(f) {
    if(!f || !Number.isInteger(f.column) || !Number.isInteger(f.row) || !['N','E','S','W'].includes(f.edge))return null;
    if(f.edge==='W')return {column:f.column-1,row:f.row,edge:'E'};
    if(f.edge==='N')return {column:f.column,row:f.row-1,edge:'S'};
    return {column:f.column,row:f.row,edge:f.edge};
  }
  function key(f) {const e=canonical(f);return e?`${e.column},${e.row},${e.edge}`:null;}
  function blocks(f,a,b) {
    if(!a || !b || Math.abs(a.column-b.column)+Math.abs(a.row-b.row)!==1)return false;
    const edge=a.column!==b.column ? {column:Math.min(a.column,b.column),row:a.row,edge:'E'} : {column:a.column,row:Math.min(a.row,b.row),edge:'S'};
    const target=key(edge);return target!==null && key(f)===target;
  }
  return Object.freeze({canonical,key,blocks});
});
