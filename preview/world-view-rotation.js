// Pure quarter-turn camera geometry. Never rotate or rewrite world records.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory(require('./ramp-geometry.js'));else root.WorldViewRotation=factory(root.RampGeometry);})(typeof window!=='undefined'?window:globalThis,(RampGeometry)=>{
  'use strict';
  const directions=['N','E','S','W'];
  const turns=n=>Number.isInteger(n)?((n%4)+4)%4:null;
  function point(column,row,size,quarterTurns) {
    const q=turns(quarterTurns);
    if(q===null || !Number.isInteger(size) || size<1 || !Number.isFinite(column) || !Number.isFinite(row))return null;
    let c=column,r=row;for(let i=0;i<q;i++){const next=size-1-r;r=c;c=next;}return {column:c,row:r};
  }
  function direction(facing,quarterTurns) {
    const q=turns(quarterTurns),i=directions.indexOf(facing);return q===null || i<0?null:directions[(i+q)%4];
  }
  function rectangle(column,row,width,height,size,quarterTurns) {
    const q=turns(quarterTurns);
    if(q===null || ![column,row,width,height,size].every(Number.isInteger) || size<1 || width<1 || height<1 || column<0 || row<0 || column+width>size || row+height>size)return null;
    let c=column,r=row,w=width,h=height;
    for(let i=0;i<q;i++){const nc=size-r-h,nr=c;[c,r,w,h]=[nc,nr,h,w];}
    return {column:c,row:r,width:w,height:h};
  }
  function fence(f,size,q) {
    if(!f || !directions.includes(f.edge) || !Number.isInteger(f.column) || !Number.isInteger(f.row) || f.column<0 || f.row<0 || f.column>=size || f.row>=size)return null;
    const p=point(f.column,f.row,size,q),edge=direction(f.edge,q);return p&&edge?{...p,edge}:null;
  }
  function bridge(b,size,q) {
    if(!b || !['E','S'].includes(b.axis) || ![3,4,5].includes(b.length??3) || !Number.isInteger(b.column) || !Number.isInteger(b.row))return null;
    const length=b.length??3,other={column:b.column+(b.axis==='E'?length-1:0),row:b.row+(b.axis==='S'?length-1:0)};
    if(b.column<0 || b.row<0 || other.column>=size || other.row>=size)return null;
    const a=point(b.column,b.row,size,q),z=point(other.column,other.row,size,q);if(!a||!z)return null;
    return {column:Math.min(a.column,z.column),row:Math.min(a.row,z.row),axis:a.row===z.row?'E':'S',length};
  }
  // Build an isolated camera-space world snapshot; the source is never changed.
  // Only known coordinate-bearing records are transformed. This is not storage.
  function projectWorld(world,quarterTurns) {
    const q=turns(quarterTurns),size=world?.gridSize;
    if(q===null || !Number.isInteger(size) || size<1 || !Array.isArray(world.tiles) || world.tiles.length!==size*size)return null;
    const copy=x=>Array.isArray(x)?x.map(copy):x && typeof x==='object'?Object.fromEntries(Object.entries(x).map(([k,v])=>[k,copy(v)])):x;
    const result=copy(world);
    function move(p){return point(p.column,p.row,size,q);}
    function grid(source){if(!Array.isArray(source) || source.length!==size*size)throw new Error('invalid grid');const output=Array(source.length);for(let r=0;r<size;r++)for(let c=0;c<size;c++){const p=point(c,r,size,q);output[p.row*size+p.column]=copy(source[r*size+c]);}return output;}
    function facing(obj){if(obj.facing!==undefined){const d=direction(obj.facing,q);if(!d)throw new Error('invalid facing');obj.facing=d;}}
    try {
      for(const name of ['orangeTrees','fences','bridges','blockOpacity','ramps'])if(world[name]!==undefined && !Array.isArray(world[name]))throw new Error('invalid records');
      result.tiles=grid(world.tiles);
      if(world.blockStacks!==undefined)result.blockStacks=grid(world.blockStacks);
      if(world.characterColumn!==undefined || world.characterRow!==undefined){const p=point(world.characterColumn,world.characterRow,size,q);if(!p || !Number.isInteger(world.characterColumn) || !Number.isInteger(world.characterRow) || world.characterColumn<0 || world.characterRow<0 || world.characterColumn>=size || world.characterRow>=size)throw new Error('invalid character');result.characterColumn=p.column;result.characterRow=p.row;}
      if(world.characterFacing!==undefined){result.characterFacing=direction(world.characterFacing,q);if(!result.characterFacing)throw new Error('invalid facing');}
      if(world.house){const h=rectangle(world.house.column,world.house.row,3,3,size,q);if(!h)throw new Error('invalid house');Object.assign(result.house,{column:h.column,row:h.row});facing(result.house);}
      for(const [i,tree] of (world.orangeTrees||[]).entries()){
        const n=tree.size||2,r=rectangle(tree.column,tree.row,n,n,size,q);if(!r)throw new Error('invalid tree');Object.assign(result.orangeTrees[i],{column:r.column,row:r.row});facing(result.orangeTrees[i]);
      }
      for(const [i,f] of (world.fences||[]).entries()){const transformed=fence(f,size,q);if(!transformed)throw new Error('invalid fence');Object.assign(result.fences[i],transformed);}
      for(const [i,b] of (world.bridges||[]).entries()){const transformed=bridge(b,size,q);if(!transformed)throw new Error('invalid bridge');const target=result.bridges[i];target.column=transformed.column;target.row=transformed.row;target.axis=transformed.axis;}
      for(const name of ['blockOpacity','ramps'])for(const [i,item] of (world[name]||[]).entries()){
        const p=move(item);if(!p || !Number.isInteger(item.column) || !Number.isInteger(item.row) || item.column<0 || item.row<0 || item.column>=size || item.row>=size)throw new Error('invalid tile record');
        Object.assign(result[name][i],p);if(name==='ramps'){const d=direction(item.direction,q);if(!d)throw new Error('invalid ramp');result[name][i].direction=d;}
      }
      return result;
    }catch(_){return null;}
  }
  // Pick the frontmost visible surface in camera space, returning WORLD coordinates.
  // Height data and ramp directions are read from the source, never transformed in place.
  function pickWorld(world,quarterTurns,x,y,originX,originY,tileWidth,tileHeight,blockHeight,baseDepth=0) {
    const q=turns(quarterTurns),size=world?.gridSize;
    if(q===null || !Number.isInteger(size) || size<1 || !Array.isArray(world.tiles) || world.tiles.length!==size*size ||
       ![x,y,originX,originY,tileWidth,tileHeight,blockHeight,baseDepth].every(Number.isFinite) || tileWidth<=0 || tileHeight<=0 || blockHeight<=0)return null;
    if(world.blockStacks!==undefined && (!Array.isArray(world.blockStacks) || world.blockStacks.length!==size*size || world.blockStacks.some(s=>!Array.isArray(s))))return null;
    if(world.ramps!==undefined && (!Array.isArray(world.ramps) || !RampGeometry))return null;
    const ramps=new Map();
    for(const r of world.ramps||[]){
      if(!r || !Number.isInteger(r.column) || !Number.isInteger(r.row) || r.column<0 || r.row<0 || r.column>=size || r.row>=size || !direction(r.direction,q) || ramps.has(`${r.column},${r.row}`))return null;
      ramps.set(`${r.column},${r.row}`,r.direction);
    }
    function worldPoint(c,r){return point(c,r,size,-q);}
    function height(c,r){if(c<0 || r<0 || c>=size || r>=size)return 0;const p=worldPoint(c,r),i=p.row*size+p.column;return world.blockStacks?world.blockStacks[i].length:(world.tiles[i]?1:0);}
    for(let diagonal=2*size-2;diagonal>=0;diagonal--)for(let r=size-1;r>=0;r--) {
      const c=diagonal-r;if(c<0 || c>=size)continue;const h=height(c,r);if(h<=0)continue;
      const p=worldPoint(c,r),cx=originX+(c-r)*tileWidth/2,cy=originY+(c+r)*tileHeight/2-(h-1)*blockHeight,dx=x-cx,hw=tileWidth/2,hh=tileHeight/2;
      const ramp=ramps.get(`${p.column},${p.row}`),viewDirection=ramp?direction(ramp,q):null;
      if(viewDirection && RampGeometry.hit(viewDirection,tileWidth,tileHeight,blockHeight,dx,y-cy))return p;
      if(Math.abs(dx)/hw+Math.abs(y-cy)/hh<=1)return p;
      if(Math.abs(dx)<=hw){
        const neighbor=height(c+(dx>=0?1:0),r+(dx<0?1:0)),face=Math.max(0,h-neighbor)*blockHeight+(neighbor===0?Math.max(0,baseDepth-blockHeight):0),edgeY=cy+hh*(1-Math.abs(dx)/hw);
        if(face>0 && y>=edgeY && y<=edgeY+face)return p;
      }
    }
    return null;
  }
  return Object.freeze({point,direction,rectangle,fence,bridge,projectWorld,pickWorld});
});
