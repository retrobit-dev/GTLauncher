// Calibration of the approved house artwork against its 3×3 tile footprint.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.HouseGeometry=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  // Measured at the lower stone corners, not at the same image row: the
  // rear-right foundation ends higher in the source artwork.
  const pivotX=256,pivotY=482,baseLeft=50,baseRight=430,baseLeftY=405,baseRightY=397;
  function create(tileWidth,tileHeight,scale=1) {
    const halfWidth=tileWidth*1.5*scale,halfDepth=tileHeight*1.5*scale;
    const sy=85/490*scale;
    const side=(edge,baseY)=>({sx:halfWidth/Math.abs(edge-pivotX), shear:(-halfDepth-(baseY-pivotY)*sy)/(edge-pivotX)});
    return {pivotX,pivotY,halfWidth,halfDepth,sy,left:side(baseLeft,baseLeftY),right:side(baseRight,baseRightY)};
  }
  function project(g,x,y) {
    const side=x<g.pivotX?g.left:g.right,dx=x-g.pivotX;
    return {x:dx*side.sx,y:(y-g.pivotY)*g.sy+dx*side.shear};
  }
  function unproject(g,x,y) {
    const side=x<0?g.left:g.right,dx=x/side.sx;
    return {x:g.pivotX+dx,y:g.pivotY+(y-dx*side.shear)/g.sy};
  }
  return Object.freeze({create,project,unproject,baseLeft,baseRight,baseLeftY,baseRightY});
});
