// A tiny isometric mesh, not a stretched illustration: top, end and front faces
// are projected from the actual tile edge. World records remain unchanged.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.FenceMesh=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function faces(a,b,material,joins={}) {
    const slope=(b.y-a.y)/(b.x-a.x),depth=material.startsWith('stone')?2.6:1.8;
    // Perpendicular tile axis, always pointing towards the viewer.
    const nx=slope>=0?-depth:depth,ny=Math.abs(slope)*depth;
    const point=(u,v,z)=>({x:a.x+(b.x-a.x)*u+nx*v,y:a.y+(b.y-a.y)*u+ny*v-z});
    const output=[];
    function box(u0,u1,v0,v1,z0,z1,palette) {
      const p=(u,v,z)=>point(u,v,z);
      // The near end switches when the edge rises toward the right.
      const near=slope>=0?u1:u0;
      output.push({color:palette.side,points:[p(near,v0,z0),p(near,v1,z0),p(near,v1,z1),p(near,v0,z1)]});
      output.push({color:palette.front,points:[p(u0,v1,z0),p(u1,v1,z0),p(u1,v1,z1),p(u0,v1,z1)]});
      output.push({color:palette.top,points:[p(u0,v0,z1),p(u1,v0,z1),p(u1,v1,z1),p(u0,v1,z1)]});
    }
    function mark(u0,u1,v,z0,z1,color) {
      output.push({color,detail:true,points:[point(u0,v,z0),point(u1,v,z0),point(u1,v,z1),point(u0,v,z1)]});
    }
    if(material==='stone' || material==='stone-tall') {
      const palettes=[{top:'#bbc2af',front:'#909a88',side:'#737e6d'},{top:'#c4c7b8',front:'#9da595',side:'#7b8675'},{top:'#b6c0ad',front:'#899583',side:'#707d6d'}];
      // Low 2-course stonework, with smaller blocks and a readable thickness.
      const boundaries=[[0,.30,.67,1],[0,.19,.51,.79,1]];
      const courses=material==='stone-tall'?26:2,courseHeight=material==='stone-tall'?127.5/26:4.5;
      for(let course=0;course<courses;course++)for(let i=0;i<boundaries[course%2].length-1;i++) {
        const gap=.006,u0=boundaries[course%2][i]+(i===0 && joins.linkA?0:gap),u1=boundaries[course%2][i+1]-(i===boundaries[course%2].length-2 && joins.linkB?0:gap),z0=course*courseHeight+.10,z1=(course+1)*courseHeight-.10-(course && i%2?.12:0);
        box(u0,u1,-.5,.5,z0,z1,palettes[(i+course)%3]);
        const chip=u0+(u1-u0)*(.26+(i%2)*.18);mark(chip,chip+(u1-u0)*.13,.505,z0+1.1,z0+1.45,'#aab3a0');
      }
      const joint={top:'#b8c1b1',front:'#929c8c',side:'#73806e'};
      const wallHeight=material==='stone-tall'?127.4:8.9;
      if(joins.joinA)box(-.09,.09,-.5,.5,0,wallHeight,joint);
      if(joins.joinB)box(.91,1.09,-.5,.5,0,wallHeight,joint);
    } else {
      const rail={top:'#c69c68',front:'#a6784f',side:'#80583b'},post={top:'#d0ab79',front:'#a67c56',side:'#79543d'};
      // Two stout rails run between posts; the square posts have visible end and top faces.
      box(joins.linkA?0:.04,joins.linkB?1:.96,-.30,.30,5.2,7.3,rail);box(joins.linkA?0:.04,joins.linkB?1:.96,-.30,.30,10.1,12.2,rail);
      if(joins.postA!==false)box(-.065,.065,-.65,.65,0,15+4*(joins.postExtraA||0),post);if(joins.postB!==false)box(.935,1.065,-.65,.65,0,15+4*(joins.postExtraB||0),post);
      mark(.18,.43,.305,6.1,6.3,'#8e694a');mark(.50,.77,.305,11,11.2,'#8e694a');
    }
    return output;
  }
  function contains(points,x,y) {
    let inside=false;
    for(let i=0,j=points.length-1;i<points.length;j=i++) {
      const a=points[i],b=points[j];
      if((a.y>y)!==(b.y>y) && x<(b.x-a.x)*(y-a.y)/(b.y-a.y)+a.x)inside=!inside;
    }
    return inside;
  }
  return Object.freeze({faces,contains});
});
