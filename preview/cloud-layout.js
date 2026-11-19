// Shared weather, sizing and island-wide cloud-shadow layout (mirrored on Android).
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.CloudLayout=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function count(cover,code){
    const value=Number.isFinite(cover)?Math.max(0,Math.min(100,cover)):0;
    if(code>=95 && code<=99)return Math.min(10,7+Math.floor(value/25));
    if(code===0||code===1)return 3;
    return Math.max(3,Math.min(6,3+Math.floor(value/25)));
  }
  // Scale with the actual world rather than a fixed four tiles on every island.
  function cellSize(w,h,cols,rows,gridSize=9,cloudCount=3){
    const span=Math.max(6,gridSize*.9/Math.sqrt(Math.max(1,cloudCount)));
    return .25*span*Math.min(w/cols,h/rows);
  }
  // Stratify one axis and use a low-discrepancy sequence for the other. All
  // centers travel around the complete island grid with the integrated wind.
  function seed(index,count,gridSize,jitterEast,jitterNorth){
    const east=(index+.5+(jitterEast-.5)*.3)/count;
    const north=((index*.6180339887498949+.5+(jitterNorth-.5)*.12)%1+1)%1;
    return {east:east*gridSize,north:north*gridSize};
  }
  function wet(cover,code){return cover>=65||code===3||code===45||code===48||code>=51;}
  function fade(seconds,animated){return animated?.6+.4*(1+Math.cos(2*Math.PI*seconds/24))/2:1;}
  return Object.freeze({count,cellSize,seed,wet,fade});
});
