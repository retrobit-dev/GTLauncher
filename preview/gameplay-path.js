// Exact-tile routing on legacy and 20x20/25x25/50x50/100x100 preview worlds, without changing saved data.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory(require('./bridge-geometry.js'),require('./ramp-geometry.js'));else root.GameplayPath=factory(root.BridgeGeometry,root.RampGeometry);})(typeof window!=='undefined'?window:globalThis,(BridgeGeometry,RampGeometry)=>{
  'use strict';
  function find(world,start,end,house,nearestDry=false) {
    const size=[20,25,50,100].includes(world?.gridSize)?world.gridSize:9;
    const inside=p=>p && Number.isInteger(p.column)&&Number.isInteger(p.row)&&p.column>=0&&p.column<size&&p.row>=0&&p.row<size;
    const keyOf=p=>p.row*size+p.column;
    // Optional elevation layer: each entry is the stack in that grid cell.
    // Legacy worlds without this field keep their existing flat routing behavior.
    const stacks=world?.blockStacks;
    if (stacks !== undefined && (!Array.isArray(stacks) || stacks.length!==size*size ||
      stacks.some(s=>!Array.isArray(s) || s.length>32 || s.some(v=>!Number.isInteger(v)||v<1||v>10)))) return null;
    const height=p=>BridgeGeometry?.deckHeight(world,p) ?? (stacks ? stacks[keyOf(p)].length : (!world.tiles || world.tiles[keyOf(p)]!==0 ? 1 : 0));
    // In-memory ramp data only; portable world support is intentionally not enabled yet.
    // Reject malformed records instead of silently routing over unsupported terrain.
    const ramps=new Map();
    if(world?.ramps!==undefined){
      if(!RampGeometry || !Array.isArray(world.ramps))return null;
      for(const ramp of world.ramps){
        if(!inside(ramp) || !RampGeometry.DIRECTIONS.includes(ramp.direction) || height(ramp)<1 || ramps.has(keyOf(ramp)))return null;
        ramps.set(keyOf(ramp),ramp);
      }
    }
    function terrainStep(a,b,edge){
      const da=ramps.get(keyOf(a))?.direction,db=ramps.get(keyOf(b))?.direction;
      return da===undefined && db===undefined ? Math.abs(height(a)-height(b))<=1 : RampGeometry.canConnect({height:height(a),direction:da},{height:height(b),direction:db},edge);
    }
    const inHouse=p=>house?.visible && p.column>=house.column && p.column<house.column+3 && p.row>=house.row && p.row<house.row+3;
    const escaping=inside(start) && inHouse(start);
    const inTree=p=>(world?.orangeTrees||[]).some(t=>t.kind!=='wild-grass' && t.kind!=='bush' && p.column>=t.column && p.column<t.column+(t.size||2) && p.row>=t.row && p.row<t.row+(t.size||2));
    const water=p=>inside(p) && (ramps.get(keyOf(p))?.material ?? world.tiles?.[keyOf(p)])===3 && !(world.bridges||[]).some(b=>BridgeGeometry?.indexAt(b,p)>=0);
    const pass=p=>inside(p) && (nearestDry || !water(p)) && !inTree(p) && height(p)>0 && (!world.tiles || world.tiles[p.row*size+p.column]!==0 || BridgeGeometry?.deckHeight(world,p)!=null) && (!inHouse(p) || escaping);
    // Older worlds may have a house placed on top of the character. Let the
    // character leave that starting tile, but never enter an occupied destination.
    const startOnTile=inside(start) && height(start)>0 && (!world.tiles || world.tiles[keyOf(start)]!==0 || BridgeGeometry?.deckHeight(world,start)!=null);
    if(!startOnTile || (!nearestDry && (!inside(end) || !pass(end) || (inHouse(end) && !(start.column===end.column && start.row===end.row)))))return null;
    const key=p=>p.row*size+p.column, queue=[start], prev=new Map([[key(start),null]]);
    for(let i=0;i<queue.length;i++) {
      const p=queue[i]; if(nearestDry ? (key(p)!==key(start) && pass(p) && !inHouse(p) && ((ramps.get(key(p))?.material ?? world.tiles?.[key(p)])!==3 || (world.bridges||[]).some(b=>BridgeGeometry?.indexAt(b,p)>=0))) : (p.column===end.column&&p.row===end.row)) {
        const route=[];let n=key(p);while(n!==null){route.push({column:n%size,row:Math.floor(n/size)});n=prev.get(n);}return route.reverse();
      }
      for(const [dx,dy,edge] of [[0,-1,'N'],[1,0,'E'],[0,1,'S'],[-1,0,'W']]) {
        const next={column:p.column+dx,row:p.row+dy};if(!pass(next)||((world.bridges||[]).length && (!BridgeGeometry || !BridgeGeometry.permitsStep(world.bridges,p,next)))||(world.fences||[]).some(f=>{const d={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[f.edge];return d && ((p.column===f.column && p.row===f.row && next.column===f.column+d[0] && next.row===f.row+d[1]) || (next.column===f.column && next.row===f.row && p.column===f.column+d[0] && p.row===f.row+d[1]));})||!terrainStep(p,next,edge)||prev.has(key(next)) || (escaping && !inHouse(p) && inHouse(next)))continue;
        prev.set(key(next),key(p));queue.push(next);
      }
    }
    return null;
  }
  function findBeside(world,start,house) {
    if(!house?.visible)return null;
    let best=null;
    for(let row=house.row-1;row<=house.row+3;row++) for(let column=house.column-1;column<=house.column+3;column++) {
      const dx=Math.max(house.column-column,0,column-(house.column+2));
      const dy=Math.max(house.row-row,0,row-(house.row+2));
      if(dx+dy!==1)continue;
      const route=find(world,start,{column,row},house);
      if(route && (!best || route.length<best.length))best=route;
    }
    return best;
  }
  function findNear(world,start,target,house) {
    let best=null;
    for(const [dx,dy] of [[0,-1],[1,0],[0,1],[-1,0]]) {
      const route=find(world,start,{column:target.column+dx,row:target.row+dy},house);
      if(route && (!best || route.length<best.length))best=route;
    }
    return best;
  }
  function findNearestDry(world,start,house) { return find(world,start,null,house,true); }
  return Object.freeze({find,findBeside,findNear,findNearestDry});
});
