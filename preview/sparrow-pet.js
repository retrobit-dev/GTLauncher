// Cosmetic companion: hovers above the player's head, then perches when seated.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.SparrowPet=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function create(){return {
    reset(){},
    update(now,world,anchor,scale,sitting,mirror,back,moving,reduced,headY){
      const seat=Math.max(0,Math.min(1,sitting));
      const hover=!reduced && seat<1;
      const cap=Number.isFinite(headY)?headY:anchor.y-24.5*scale;
      const drift=hover?Math.sin(now/850)*1.6*scale:0;
      const bob=hover?Math.sin(now/450)*1.2*scale:0;
      const x=anchor.x+drift*(1-seat)+(mirror?-1:1)*.5*scale*seat;
      const y=cap+(-18*scale+bob)*(1-seat);
      return {x,y,seat,hovering:hover,flap:hover && Math.floor(now/120)%2===0};
    }
  };}
  return Object.freeze({create});
});
