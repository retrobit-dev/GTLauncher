// Exact-tile routing on legacy and 20x20/25x25/50x50/100x100 preview worlds, without changing saved data.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory(require('./bridge-geometry.js'),require('./ramp-geometry.js'));else root.GameplayPath=factory(root.BridgeGeometry,root.RampGeometry);})(typeof window!=='undefined'?window:globalThis,(BridgeGeometry,RampGeometry)=>{
  'use strict';
  // Entering a road costs 3; other tiles cost 4. This favors a modest
  // detour on roads while retaining a strict 4/3 bound versus shortest paths.
  function roadKeys(world,size) {return new Set((world?.roads||[]).filter(r=>r.material!=='water').map(r=>r.row*size+r.column));}
  function score(world,route) {
    if(!route)return Infinity;
    const size=[20,25,50,100].includes(world?.gridSize)?world.gridSize:9,roads=roadKeys(world,size);
    return route.slice(1).reduce((n,p)=>n+(roads.has(p.row*size+p.column)?3:4),0);
  }
  function betterRoute(world,a,b) {if(!a)return false;if(!b)return true;const x=score(world,a),y=score(world,b);return x<y || (x===y && a.length<b.length);}
  class MinHeap {
    constructor(){this.items=[];}
    static less(a,b){return a.cost<b.cost || (a.cost===b.cost && (a.steps<b.steps || (a.steps===b.steps && a.order<b.order)));}
    push(v){const a=this.items;a.push(v);let i=a.length-1;while(i>0){const p=(i-1)>>1;if(!MinHeap.less(a[i],a[p]))break;[a[i],a[p]]=[a[p],a[i]];i=p;}}
    pop(){const a=this.items,root=a[0],last=a.pop();if(a.length){a[0]=last;let i=0;for(;;){let n=i,l=2*i+1,r=l+1;if(l<a.length && MinHeap.less(a[l],a[n]))n=l;if(r<a.length && MinHeap.less(a[r],a[n]))n=r;if(n===i)break;[a[i],a[n]]=[a[n],a[i]];i=n;}}return root;}
    get length(){return this.items.length;}
  }
  function find(world,start,end,house,nearestDry=false) {
    const size=[20,25,50,100].includes(world?.gridSize)?world.gridSize:9;
    const inside=p=>p && Number.isInteger(p.column)&&Number.isInteger(p.row)&&p.column>=0&&p.column<size&&p.row>=0&&p.row<size;
    const keyOf=p=>p.row*size+p.column;
    // Optional elevation layer: each entry is the stack in that grid cell.
    // Legacy worlds without this field keep their existing flat routing behavior.
    const stacks=world?.blockStacks;
    if (stacks !== undefined && (!Array.isArray(stacks) || stacks.length!==size*size ||
      stacks.some(s=>!Array.isArray(s) || s.some(v=>!Number.isInteger(v)||v<0||v>10)))) return null;
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
      const da=BridgeGeometry?.deckHeight(world,a)==null ? ramps.get(keyOf(a))?.direction : null,db=BridgeGeometry?.deckHeight(world,b)==null ? ramps.get(keyOf(b))?.direction : null;
      return da===undefined && db===undefined ? Math.abs(height(a)-height(b))<=1 : RampGeometry.canConnect({height:height(a),direction:da},{height:height(b),direction:db},edge);
    }
    const inHouse=p=>house?.visible && p.column>=house.column && p.column<house.column+3 && p.row>=house.row && p.row<house.row+3;
    const escaping=inside(start) && inHouse(start);
    const inTree=p=>(world?.orangeTrees||[]).some(t=>t.kind!=='wild-grass' && t.kind!=='bush' && p.column>=t.column && p.column<t.column+(t.size||2) && p.row>=t.row && p.row<t.row+(t.size||2));
    const river=new Set((world?.roads||[]).filter(r=>r.material==='water').map(r=>keyOf(r)));
    const water=p=>inside(p) && ((ramps.get(keyOf(p))?.material ?? world.tiles?.[keyOf(p)])===3 || river.has(keyOf(p))) && !(world.bridges||[]).some(b=>BridgeGeometry?.indexAt(b,p)>=0);
    const pass=p=>inside(p) && (nearestDry || !water(p)) && !inTree(p) && height(p)>0 && (!world.tiles || world.tiles[p.row*size+p.column]!==0 || BridgeGeometry?.deckHeight(world,p)!=null) && (!inHouse(p) || escaping);
    // Older worlds may have a house placed on top of the character. Let the
    // character leave that starting tile, but never enter an occupied destination.
    const startOnTile=inside(start) && height(start)>0 && (!world.tiles || world.tiles[keyOf(start)]!==0 || BridgeGeometry?.deckHeight(world,start)!=null);
    if(!startOnTile || (!nearestDry && (!inside(end) || !pass(end) || (inHouse(end) && !(start.column===end.column && start.row===end.row)))))return null;
    const key=p=>p.row*size+p.column,roads=roadKeys(world,size),prev=new Map([[key(start),null]]),best=new Map([[key(start),{cost:0,steps:0}]]),queue=new MinHeap();
    let order=0;queue.push({p:start,cost:0,steps:0,order:order++});
    while(queue.length) {
      const item=queue.pop(),p=item.p,known=best.get(key(p));if(item.cost!==known.cost || item.steps!==known.steps)continue;
      if(nearestDry ? (key(p)!==key(start) && pass(p) && !inHouse(p) && (!water(p) || (world.bridges||[]).some(b=>BridgeGeometry?.indexAt(b,p)>=0))) : (p.column===end.column&&p.row===end.row)) {
        const route=[];let n=key(p);while(n!==null){route.push({column:n%size,row:Math.floor(n/size)});n=prev.get(n);}return route.reverse();
      }
      for(const [dx,dy,edge] of [[0,-1,'N'],[1,0,'E'],[0,1,'S'],[-1,0,'W']]) {
        const next={column:p.column+dx,row:p.row+dy};if(!pass(next)||((world.bridges||[]).length && (!BridgeGeometry || !BridgeGeometry.permitsStep(world.bridges,p,next)))||(world.fences||[]).some(f=>{if(f.material?.startsWith('gate-'))return false;const d={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[f.edge];return d && ((p.column===f.column && p.row===f.row && next.column===f.column+d[0] && next.row===f.row+d[1]) || (next.column===f.column && next.row===f.row && p.column===f.column+d[0] && p.row===f.row+d[1]));})||!terrainStep(p,next,edge) || (escaping && !inHouse(p) && inHouse(next)))continue;
        const k=key(next),cost=item.cost+(nearestDry?1:roads.has(k)?3:4),steps=item.steps+1,old=best.get(k);
        if(old && (old.cost<cost || (old.cost===cost && old.steps<=steps)))continue;
        best.set(k,{cost,steps});prev.set(k,key(p));queue.push({p:next,cost,steps,order:order++});
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
      if(betterRoute(world,route,best))best=route;
    }
    return best;
  }
  function findNear(world,start,target,house) {
    let best=null;
    for(const [dx,dy] of [[0,-1],[1,0],[0,1],[-1,0]]) {
      const route=find(world,start,{column:target.column+dx,row:target.row+dy},house);
      if(betterRoute(world,route,best))best=route;
    }
    return best;
  }
  function findNearestDry(world,start,house) { return find(world,start,null,house,true); }
  return Object.freeze({find,findBeside,findNear,findNearestDry,score});
});
