// Pure bridge footprint and edge rules. No world schema or rendering is changed here.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.BridgeGeometry=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const LENGTH=3;
  function cells(bridge){
    if(!bridge || !Number.isInteger(bridge.column) || !Number.isInteger(bridge.row) || !['E','S'].includes(bridge.axis) || !['wood','stone'].includes(bridge.material))return null;
    return Array.from({length:LENGTH},(_,i)=>({column:bridge.column+(bridge.axis==='E'?i:0),row:bridge.row+(bridge.axis==='S'?i:0)}));
  }
  function indexAt(bridge,p){const c=cells(bridge);return c?c.findIndex(t=>t.column===p.column && t.row===p.row):-1;}
  // All three cells must be supported by level terrain, clear of objects and
  // other bridges. Endpoints must be dry; the middle may be dry or water.
  function valid(world,bridges){
    const size=world?.gridSize;
    if(!Number.isInteger(size) || !Array.isArray(world.tiles) || world.tiles.length!==size*size || !Array.isArray(bridges))return false;
    const occupied=new Set(),house=world.house;
    for(const bridge of bridges){
      if(!bridge || Object.keys(bridge).some(k=>!['column','row','axis','material'].includes(k)))return false;
      const footprint=cells(bridge);if(!footprint)return false;let height=null;
      for(let i=0;i<footprint.length;i++){
        const p=footprint[i],{column:c,row:r}=p;if(c<0 || r<0 || c>=size || r>=size)return false;
        const idx=r*size+c,material=world.tiles[idx],h=world.blockStacks?world.blockStacks[idx]?.length:(material?1:0);
        if(!material || !h || (i!==1 && material===3) || (height!==null && height!==h) || occupied.has(idx))return false;
        if(house && c>=house.column && c<house.column+3 && r>=house.row && r<house.row+3)return false;
        if((world.orangeTrees||[]).some(t=>c>=t.column && c<t.column+(t.size||2) && r>=t.row && r<t.row+(t.size||2)))return false;
        if((world.fences||[]).some(f=>{const d={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[f.edge];return (f.column===c && f.row===r) || (d && f.column+d[0]===c && f.row+d[1]===r);}))return false;
        height=h;occupied.add(idx);
      }
    }
    return true;
  }
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
  return Object.freeze({LENGTH,cells,indexAt,valid,permitsStep});
});
