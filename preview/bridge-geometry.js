// Shared bridge footprint and end-only traversal rules.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.BridgeGeometry=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const LENGTH=3;
  function cells(bridge){
    if(!bridge || !Number.isInteger(bridge.column) || !Number.isInteger(bridge.row) || !['E','S'].includes(bridge.axis) || !['wood','stone'].includes(bridge.material) || ![3,4,5].includes(bridge.length??3) || (bridge.width!==undefined && bridge.width!==((bridge.length??3)===5?3:2)))return null;
    const width=bridge.width??1;return Array.from({length:(bridge.length??3)*width},(_,n)=>{const i=Math.floor(n/width),j=n%width;return {column:bridge.column+(bridge.axis==='E'?i:j),row:bridge.row+(bridge.axis==='S'?i:j)};});
  }
  function indexAt(bridge,p){const c=cells(bridge);const n=c?c.findIndex(t=>t.column===p.column && t.row===p.row):-1;return n<0?-1:Math.floor(n/(bridge.width??1));}
  // All footprint cells must be supported by level terrain, clear of objects and
  // other bridges. Endpoints must be dry; interior cells may be dry or water.
  function valid(world,bridges){
    const size=world?.gridSize;
    if(!Number.isInteger(size) || !Array.isArray(world.tiles) || world.tiles.length!==size*size || !Array.isArray(bridges))return false;
    const occupied=new Set(),house=world.house;
    for(const bridge of bridges){
      if(!bridge || Object.keys(bridge).some(k=>!['column','row','axis','material','length','width','opacity'].includes(k)) || (bridge.opacity!==undefined && (!Number.isInteger(bridge.opacity) || bridge.opacity<0 || bridge.opacity>90 || bridge.opacity%5!==0)) || (Object.hasOwn(bridge,'length') && ![4,5].includes(bridge.length)))return false;
      const footprint=cells(bridge);if(!footprint)return false;let height=null;
      for(let i=0;i<footprint.length;i++){
        const p=footprint[i],{column:c,row:r}=p;if(c<0 || r<0 || c>=size || r>=size)return false;
        const idx=r*size+c,material=world.tiles[idx],h=world.blockStacks?world.blockStacks[idx]?.length:(material?1:0);
        if(!material || !h || ((Math.floor(i/(bridge.width??1))===0 || Math.floor(i/(bridge.width??1))===(bridge.length??3)-1) && material===3) || (height!==null && height!==h) || occupied.has(idx))return false;
        if(house && c>=house.column && c<house.column+3 && r>=house.row && r<house.row+3)return false;
        if((world.orangeTrees||[]).some(t=>c>=t.column && c<t.column+(t.size||2) && r>=t.row && r<t.row+(t.size||2)))return false;
        if((world.fences||[]).some(f=>{const d={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[f.edge];return (f.column===c && f.row===r) || (d && f.column+d[0]===c && f.row+d[1]===r);}))return false;
        height=h;occupied.add(idx);
      }
    }
    return true;
  }
  // Side railings prevent entering or leaving except through the two end tiles.
  function permitsStep(bridges,from,to){
    if(!from || !to || Math.abs(from.column-to.column)+Math.abs(from.row-to.row)!==1)return false;
    for(const b of bridges||[]){const a=indexAt(b,from),z=indexAt(b,to);if(a<0 && z<0)continue;
      if(a>=0 && z>=0)continue; // movement within the deck is allowed in both directions
      const i=a>=0?a:z,other=a>=0?to:from,inside=a>=0?from:to;
      if(b.axis==='E') {if(other.row!==inside.row || !((i===0 && other.column===b.column-1)||(i===(b.length??3)-1 && other.column===b.column+(b.length??3))))return false;}
      else if(other.column!==inside.column || !((i===0 && other.row===b.row-1)||(i===(b.length??3)-1 && other.row===b.row+(b.length??3))))return false;
    }
    return true;
  }
  // Deck corner landmarks in the transparent source sprites (lower left, far left,
  // lower right). Map the deck, not the bitmap bounds or its raised rails, to the
  // exact isometric footprint; the same affine map preserves posts and arch.
  function spriteTransform(material,length,tileWidth,tileHeight,width=1){
    const [p,u,v]=width===1?(material==='wood'?[[0,284],[430,47],[131,359]]:[[35,253],[450,50],[158,321]]):[[50,250],[335,85],[176,340]];
    const ux=u[0]-p[0],uy=u[1]-p[1],vx=v[0]-p[0],vy=v[1]-p[1],det=ux*vy-uy*vx;
    const lx=length*tileWidth/2,ly=-length*tileHeight/2,wx=width*tileWidth/2,wy=width*tileHeight/2;
    const a=(lx*vy-wx*uy)/det,c=(wx*ux-lx*vx)/det;
    const b=(ly*vy-wy*uy)/det,d=(wy*ux-ly*vx)/det;
    const cx=p[0]+(ux+vx)/2,cy=p[1]+(uy+vy)/2;
    return {a,b,c,d,e:-a*cx-c*cy,f:-b*cx-d*cy};
  }
  // Construct deck and posts in world space, so vertical posts are never sheared.
  function wideDeck(length,width,tileWidth,tileHeight) {
    const u={x:length*tileWidth/2,y:-length*tileHeight/2},v={x:width*tileWidth/2,y:width*tileHeight/2};
    const p={x:-(u.x+v.x)/2,y:-(u.y+v.y)/2};
    return [p,{x:p.x+u.x,y:p.y+u.y},{x:p.x+u.x+v.x,y:p.y+u.y+v.y},{x:p.x+v.x,y:p.y+v.y}];
  }
  function polygonContains(point,poly) {let inside=false;for(let i=0,j=poly.length-1;i<poly.length;j=i++) {const a=poly[i],b=poly[j];if((a.y>point.y)!==(b.y>point.y) && point.x<(b.x-a.x)*(point.y-a.y)/(b.y-a.y)+a.x)inside=!inside;}return inside;}
  function postContains(p,posts) {return posts.some(a=>{const dx=Math.abs(p.x-a.x),dy=p.y-(a.y+2);return dx<=2 && dy>=-10.5+dx/2 && dy<=-dx/2;});}
  function wideContains(point,corners) {
    if(postContains(point,corners) || polygonContains(point,corners))return true;
    const [a,b,c,d]=corners;return polygonContains(point,[d,c,{x:c.x,y:c.y+2.5},{x:d.x,y:d.y+2.5}]) || polygonContains(point,[b,c,{x:c.x,y:c.y+2.5},{x:b.x,y:b.y+2.5}]);
  }
  return Object.freeze({LENGTH,cells,indexAt,valid,permitsStep,spriteTransform,wideDeck,wideContains});
});
