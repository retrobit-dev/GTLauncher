// Direction is derived from neighbouring water, without adding data to old saves.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.WaterFlow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const cache=new WeakMap();
  const steps={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]};
  function direction(world,c,r){
    const size=world?.gridSize,tiles=world?.tiles;if(!tiles || !Number.isInteger(size) || c<0 || r<0 || c>=size || r>=size || tiles[r*size+c]!==3)return null;
    let entry=cache.get(world);if(!entry){entry=new Map();cache.set(world,entry);}const key=r*size+c;if(entry.has(key))return entry.get(key);
    const water=(x,y)=>x>=0&&y>=0&&x<size&&y<size&&tiles[y*size+x]===3;
    const height=(x,y)=>world.blockStacks?.[y*size+x]?.length ?? 1,here=height(c,r);
    // Prefer a neighbouring downhill surface; otherwise follow the longest axis.
    let downhill=null,bestDrop=0;
    for(const [d,[dx,dy]] of Object.entries(steps)){const x=c+dx,y=r+dy;if(water(x,y)){const drop=here-height(x,y);if(drop>bestDrop){bestDrop=drop;downhill=d;}}}
    if(downhill){entry.set(key,downhill);return downhill;}
    const count=(dx,dy)=>{let n=0,x=c+dx,y=r+dy;while(water(x,y)){n++;x+=dx;y+=dy;}return n;};
    const e=count(1,0),w=count(-1,0),s=count(0,1),n=count(0,-1);
    const result=e+w>=n+s?'E':'S';entry.set(key,result);return result;
  }
  return Object.freeze({direction});
});
