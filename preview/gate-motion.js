// Stateless opening amount based on the player's distance from a tile edge.
// The gate is fully open within half a tile and closes smoothly by one tile.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.GateMotion=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function openness(gate,player) {
    if(!gate || !player || !Number.isFinite(gate.column) || !Number.isFinite(gate.row) || !Number.isFinite(player.column) || !Number.isFinite(player.row))return 0;
    const {column:c,row:r,edge}=gate;
    let x1,y1,x2,y2;
    if(edge==='N'){x1=c-.5;y1=r-.5;x2=c+.5;y2=y1;}
    else if(edge==='S'){x1=c-.5;y1=r+.5;x2=c+.5;y2=y1;}
    else if(edge==='W'){x1=c-.5;y1=r-.5;x2=x1;y2=r+.5;}
    else if(edge==='E'){x1=c+.5;y1=r-.5;x2=x1;y2=r+.5;}
    else return 0;
    const dx=x2-x1,dy=y2-y1,t=Math.max(0,Math.min(1,((player.column-x1)*dx+(player.row-y1)*dy)/(dx*dx+dy*dy)));
    const distance=Math.hypot(player.column-(x1+t*dx),player.row-(y1+t*dy));
    const p=Math.max(0,Math.min(1,(1-distance)*2));return p*p*(3-2*p);
  }
  // Remember the approach side until the player has moved out of range, so
  // the leaf never reverses through the player halfway across the threshold.
  function update(gate,player,previousSide=0) {
    const amount=openness(gate,player);
    if(!amount)return {amount:0,side:0};
    const vertical=gate.edge==='E'||gate.edge==='W';
    const boundary=vertical ? gate.column+(gate.edge==='E'?.5:-.5) : gate.row+(gate.edge==='S'?.5:-.5);
    const position=vertical ? player.column : player.row;
    const side=previousSide===1 || previousSide===-1 ? previousSide : position<boundary?-1:1;
    return {amount,side};
  }
  return Object.freeze({openness,update});
});
