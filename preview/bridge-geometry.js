// Pure bridge footprint and edge rules. No world schema or rendering is changed here.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.BridgeGeometry=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const LENGTH=3;
  function cells(bridge){
    if(!bridge || !Number.isInteger(bridge.column) || !Number.isInteger(bridge.row) || !['E','S'].includes(bridge.axis) || !['wood','stone'].includes(bridge.material))return null;
    return Array.from({length:LENGTH},(_,i)=>({column:bridge.column+(bridge.axis==='E'?i:0),row:bridge.row+(bridge.axis==='S'?i:0)}));
  }
  function indexAt(bridge,p){const c=cells(bridge);return c?c.findIndex(t=>t.column===p.column && t.row===p.row):-1;}
  // Side railings prevent entering or leaving except through the two end tiles.
  function permitsStep(bridges,from,to){
    if(!from || !to || Math.abs(from.column-to.column)+Math.abs(from.row-to.row)!==1)return false;
    for(const b of bridges||[]){const a=indexAt(b,from),z=indexAt(b,to);if(a<0 && z<0)continue;
      if(a>=0 && z>=0){if(Math.abs(a-z)!==1)return false;continue;}
      const i=a>=0?a:z,other=a>=0?to:from, footprint=cells(b),end=footprint[i];
      if(!((i===0 && (b.axis==='E'?other.column===end.column-1 && other.row===end.row:other.row===end.row-1 && other.column===end.column)) ||
           (i===LENGTH-1 && (b.axis==='E'?other.column===end.column+1 && other.row===end.row:other.row===end.row+1 && other.column===end.column))))return false;
    }
    return true;
  }
  return Object.freeze({LENGTH,cells,indexAt,permitsStep});
});
