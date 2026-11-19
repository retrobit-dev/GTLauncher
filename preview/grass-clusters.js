// Visual joins only. Each saved tuft remains an independently editable object.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.GrassClusters=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function pairs(objects,height) {
    const grass=new Map();for(const t of objects||[])if(t.kind==='wild-grass')grass.set(`${t.column},${t.row}`,t);
    const joins=[];
    for(const a of grass.values())for(const [dx,dy] of [[1,0],[0,1]]) {
      const b=grass.get(`${a.column+dx},${a.row+dy}`);
      if(b && height(a.column,a.row)===height(b.column,b.row))joins.push([a,b]);
    }
    return joins;
  }
  return Object.freeze({pairs});
});
