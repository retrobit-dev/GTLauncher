// Visual junctions only: saved segments stay independent. Different materials or
// levels never share a post. Vertices are integer half-tile coordinates.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.FenceJoins=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function vertices(f) {
    const c=f.column*2,r=f.row*2;
    switch(f.edge) {
      case 'N':return [[c-1,r-1],[c+1,r-1]];
      case 'E':return [[c+1,r+1],[c+1,r-1]];
      case 'S':return [[c-1,r+1],[c+1,r+1]];
      case 'W':return [[c-1,r+1],[c-1,r-1]];
      default:return [];
    }
  }
  function plan(fences,height) {
    const groups=new Map(),result=fences.map(()=>({postA:true,postB:true,joinA:false,joinB:false,linkA:false,linkB:false}));
    fences.forEach((f,i)=>vertices(f).forEach((v,end)=>{
      const key=`${f.material}:${height(f.column,f.row)}:${v[0]},${v[1]}`;
      if(!groups.has(key))groups.set(key,[]);
      groups.get(key).push({i,end,axis:f.edge==='N'||f.edge==='S'?0:1,depth:f.column+f.row+((f.edge==='S'||f.edge==='E')?.5:-.5)});
    }));
    for(const group of groups.values()) {
      if(group.length<2)continue;
      group.sort((a,b)=>a.depth-b.depth || a.i-b.i || a.end-b.end);
      const owner=group[group.length-1],turn=group.some(v=>v.axis!==owner.axis);
      for(const v of group) {
        const suffix=v.end?'B':'A';result[v.i]['link'+suffix]=true;
        result[v.i]['post'+suffix]=v===owner;
        result[v.i]['join'+suffix]=turn && v===owner;
      }
    }
    return result;
  }
  return Object.freeze({vertices,plan});
});
