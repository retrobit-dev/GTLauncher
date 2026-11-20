// Road surface primitives; this module does not read or mutate world data.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.RoadSurface=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const materials=Object.freeze(['dirt','stone','sand','paving','asphalt','water']);
  const palettes=Object.freeze({
    dirt:['#a66d47','#d49b6a','#765036'], stone:['#8c8c84','#bdbbb0','#5f655f'],
    sand:['#e0bd81','#f3d7a3','#bd9464'], paving:['#806b66','#c18d7e','#625b59'],
    asphalt:['#3d4143','#73716b','#262d31'], water:['#468fb7','#a3d7e4','#2d6e9a']
  });
  const valid=m=>materials.includes(m);
  const isWater=m=>m==='water';
  // A road is a one-tile surface, independent from its supporting block.
  function validRecords(roads,size) {
    if(!Number.isInteger(size)||size<1||!Array.isArray(roads)||roads.length>size*size)return false;
    const seen=new Set();
    for(const road of roads) {
      if(!road || typeof road!=='object' || Array.isArray(road) || Object.keys(road).some(k=>!['column','row','material','direction'].includes(k)) ||
        !Number.isInteger(road.column)||!Number.isInteger(road.row)||road.column<0||road.row<0||road.column>=size||road.row>=size||!valid(road.material) || (road.direction!==undefined && (road.material!=='water' || !['N','E','S','W'].includes(road.direction))))return false;
      const key=road.row*size+road.column;if(seen.has(key))return false;seen.add(key);
    }
    return true;
  }
  function put(roads,size,road,canPlace) {
    if(!validRecords(roads,size)||typeof canPlace!=='function'||!validRecords([road],size)||!canPlace(road))return null;
    if(roads.some(r=>r.column===road.column&&r.row===road.row))return null;
    return [...roads.map(r=>({...r})),{...road}];
  }
  function neighbors(roads,column,row,material) {
    const has=(c,r)=>roads.some(o=>o.column===c&&o.row===r&&o.material===material);
    return {N:has(column,row-1),E:has(column+1,row),S:has(column,row+1),W:has(column-1,row)};
  }
  // Each point is in tile-local coordinates; adjoining identical surfaces meet at edges.
  function outline(connected={}) {
    const inset=.10, l=connected.W?0:inset,r=connected.E?1:1-inset,t=connected.N?0:inset,b=connected.S?1:1-inset;
    // Cut missing diagonal corners as orthogonal steps. Never fill the corner
    // of a diagonal-only tile or create a diagonal bevel at an L/T junction.
    const p=[];
    if(connected.W && connected.N && !connected.NW)p.push([l,t+inset],[l+inset,t+inset],[l+inset,t]);else p.push([l,t]);
    if(connected.E && connected.N && !connected.NE)p.push([r-inset,t],[r-inset,t+inset],[r,t+inset]);else p.push([r,t]);
    if(connected.E && connected.S && !connected.SE)p.push([r,b-inset],[r-inset,b-inset],[r-inset,b]);else p.push([r,b]);
    if(connected.W && connected.S && !connected.SW)p.push([l+inset,b],[l+inset,b-inset],[l,b-inset]);else p.push([l,b]);
    return p;
  }
  // Transform flow coordinates with the current view-facing direction.
  function flowPoint(u,v,direction='S') {
    switch(direction){case 'N':return [1-u,1-v];case 'E':return [v,1-u];case 'W':return [1-v,u];default:return [u,v];}
  }
  function flowLines(time=0,direction='S') {
    const phase=((Number.isFinite(time)?time:0)*.16)%1;
    return [0,1,2,3].map(i=>{const v=((i*.27+phase)%1+1)%1;return [[.16,v],[.38,v-.025],[.63,v],[.84,v-.025]].map(([u,y])=>flowPoint(u,y,direction));});
  }
  function draw(ctx,options) {
    const {x,y,width,height,material,column=0,row=0,time=0,connected={},direction='S'}=options;
    if(!ctx || !valid(material) || ![x,y,width,height].every(Number.isFinite) || width<=0 || height<=0)return false;
    const hw=width/2,hh=height/2,project=(u,v)=>[x+(u-v)*hw,y+(u+v-1)*hh];
    const polygon=points=>{ctx.beginPath();points.forEach(([u,v],i)=>{const [px,py]=project(u,v);if(i===0)ctx.moveTo(px,py);else ctx.lineTo(px,py);});ctx.closePath();};
    const line=(coords,color,w=.55)=>{ctx.beginPath();coords.forEach(([u,v],i)=>{const [px,py]=project(u,v);if(!i)ctx.moveTo(px,py);else ctx.lineTo(px,py);});ctx.strokeStyle=color;ctx.lineWidth=w;ctx.stroke();};
    const dot=(u,v,color,w=1,h=.55)=>{const [px,py]=project(u,v);ctx.fillStyle=color;ctx.fillRect(px,py,w,h);};
    const [base,light,dark]=palettes[material];ctx.save();polygon(outline(connected));ctx.fillStyle=base;ctx.fill();ctx.clip();
    if(material==='stone'||material==='paving') {
      const rows=material==='stone'?3:4,cols=material==='stone'?3:4;
      for(let r=0;r<rows;r++)for(let c=-1;c<cols;c++){
        const shift=r%2?.5:0,gap=material==='stone'?.025:.015,u=(c+shift)/cols,v=r/rows;
        polygon([[u+gap,v+gap],[(c+1+shift)/cols-gap,v+gap],[(c+1+shift)/cols-gap,(r+1)/rows-gap],[u+gap,(r+1)/rows-gap]]);
        ctx.fillStyle=(r+c)%3===0?light:(r+c)%3===1?base:dark;ctx.fill();
      }
    } else if(material==='water') {
      for(const points of flowLines(time,direction))line(points,light,.65);
      const [u,v]=flowPoint(.19,.68,direction);dot(u,v,dark,1.3,.5);
    } else if(material==='sand') {
      for(let i=0;i<3;i++){const v=.22+i*.25;line([[.14,v],[.34,v-.04],[.59,v+.01],[.86,v-.03]],light,.75);}
      for(let i=0;i<5;i++)dot((i*.31+.13)%1,(i*.47+.21)%1,dark,.65,.4);
    } else {
      const seed=(column*17+row*29)>>>0,count=material==='asphalt'?16:10;
      for(let i=0;i<count;i++){const u=.12+((seed+i*37)%75)/100,v=.12+((seed*3+i*53)%75)/100;dot(u,v,i%3?light:dark,material==='asphalt'?.65:1,.45);}
      if(material==='dirt'){line([[.25,.23],[.29,.30]],dark,.8);line([[.48,.51],[.52,.58]],dark,.8);}
      else line([[.35,.28],[.43,.38],[.39,.46]],dark,.4);
    }
    ctx.restore();return true;
  }
  return Object.freeze({materials,valid,isWater,validRecords,put,neighbors,outline,flowPoint,flowLines,draw});
});
