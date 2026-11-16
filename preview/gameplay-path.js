// Exact-tile routing on legacy and 20x20/25x25 preview worlds, without changing saved data.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.GameplayPath=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function find(world,start,end,house) {
    const size=[20,25].includes(world?.gridSize)?world.gridSize:9;
    const inside=p=>p && Number.isInteger(p.column)&&Number.isInteger(p.row)&&p.column>=0&&p.column<size&&p.row>=0&&p.row<size;
    const keyOf=p=>p.row*size+p.column;
    const inHouse=p=>house?.visible && p.column>=house.column && p.column<house.column+3 && p.row>=house.row && p.row<house.row+3;
    const escaping=inside(start) && inHouse(start);
    const pass=p=>inside(p) && (!world.tiles || world.tiles[p.row*size+p.column]!==0) && (!inHouse(p) || escaping);
    // Older worlds may have a house placed on top of the character. Let the
    // character leave that starting tile, but never enter an occupied destination.
    const startOnTile=inside(start) && (!world.tiles || world.tiles[keyOf(start)]!==0);
    if(!startOnTile || !inside(end) || !pass(end) || (inHouse(end) && !(start.column===end.column && start.row===end.row)))return null;
    const key=p=>p.row*size+p.column, queue=[start], prev=new Map([[key(start),null]]);
    for(let i=0;i<queue.length;i++) {
      const p=queue[i]; if(p.column===end.column&&p.row===end.row) {
        const route=[];let n=key(p);while(n!==null){route.push({column:n%size,row:Math.floor(n/size)});n=prev.get(n);}return route.reverse();
      }
      for(const [dx,dy] of [[0,-1],[1,0],[0,1],[-1,0]]) {
        const next={column:p.column+dx,row:p.row+dy};if(!pass(next)||prev.has(key(next)))continue;
        prev.set(key(next),key(p));queue.push(next);
      }
    }
    return null;
  }
  return Object.freeze({find});
});
