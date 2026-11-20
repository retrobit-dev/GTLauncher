// Deterministic, non-persistent decorations on connected, level water surfaces.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.PondLife=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function build(world) {
    const size=world?.gridSize,tiles=world?.tiles,stacks=world?.blockStacks;
    const byTile=new Map(),ponds=[];
    if (!Number.isInteger(size) || size<1 || !Array.isArray(tiles) || tiles.length!==size*size) return {ponds,byTile};
    const height=i=>Array.isArray(stacks?.[i]) ? stacks[i].length : 1;
    const visited=new Uint8Array(tiles.length);
    for(let start=0;start<tiles.length;start++) {
      if(visited[start] || tiles[start]!==3)continue;
      const level=height(start),queue=[start];visited[start]=1;
      for(let k=0;k<queue.length;k++) {
        const i=queue[k],x=i%size,y=Math.floor(i/size);
        for(const [dx,dy] of [[0,-1],[1,0],[0,1],[-1,0]]) {
          const c=x+dx,r=y+dy,j=r*size+c;
          if(c>=0 && r>=0 && c<size && r<size && !visited[j] && tiles[j]===3 && height(j)===level){visited[j]=1;queue.push(j);}
        }
      }
      const hasSquare=queue.some(i=>i%size<size-1 && Math.floor(i/size)<size-1 &&
        [i+1,i+size,i+size+1].every(j=>tiles[j]===3 && height(j)===level));
      if(!hasSquare)continue;
      queue.sort((a,b)=>a-b);
      const units=Math.floor(queue.length/4), pond={level,area:queue.length,fish:units*2,lilypads:units};
      ponds.push(pond);
      function place(type,index,total,offset) {
        const cell=queue[Math.floor(((index+offset)*queue.length)/total)%queue.length];
        const item={type,seed:(start*131+index*37+(type==='fish'?17:71))>>>0};
        if(!byTile.has(cell))byTile.set(cell,[]);byTile.get(cell).push(item);
      }
      for(let i=0;i<pond.fish;i++)place('fish',i,pond.fish,.2);
      for(let i=0;i<pond.lilypads;i++)place('lilypad',i,pond.lilypads,.6);
    }
    return {ponds,byTile};
  }
  // Reflected periodic motion is bounded in local tile coordinates, including
  // when wind stops or changes, so decorations cannot drift onto land.
  function position(item,time,windX=0,windY=0,reduced=false) {
    const phase=(item.seed%997)/997*Math.PI*2;
    if(item.type==='fish') {
      const a=reduced ? phase : phase+time*(.6+(item.seed%3)*.1);
      return {u:.17*Math.cos(a),v:.13*Math.sin(a),direction:Math.sin(a)>=0?1:-1};
    }
    return {u:.16*Math.sin(windX*.10+phase),v:.16*Math.sin(windY*.10+phase*1.17),direction:1};
  }
  return Object.freeze({build,position});
});
