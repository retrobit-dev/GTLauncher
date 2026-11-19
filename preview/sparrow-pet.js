// Cosmetic companion: follows in scene space and perches on the seated player's cap.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.SparrowPet=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function create(){let x=0,y=0,last=null,key=null;return {
    reset(){key=null;last=null;},
    update(now,world,anchor,scale,sitting,mirror,back,moving,reduced){
      const seat=Math.max(0,Math.min(1,sitting));
      const sx=anchor.x+(mirror?-1:1)*.5*scale;
      const sy=anchor.y+(.5+(800-(back?778:725))/800*25*seat-25*(back?.88:.915))*scale;
      // Hover near the player while idle; the seated perch has priority.
      const hovering=!reduced && !moving && seat<1;
      const idleX=anchor.x+(-7+(hovering?Math.sin(now/1100)*2:0))*scale;
      const idleY=anchor.y+(.5+(hovering?-12+Math.sin(now/600):0))*scale;
      const tx=idleX+(sx-idleX)*seat;
      const ty=idleY+(sy-idleY)*seat;
      const dt=last===null?0:Math.max(0,Math.min(.25,(now-last)/1000));
      if(key!==world || last===null || now<last || Math.hypot(tx-x,ty-y)>80*scale || reduced){x=tx;y=ty;}
      else {const f=1-Math.exp(-dt*5);x+=(tx-x)*f;y+=(ty-y)*f;}
      key=world;last=now;
      const hop=!reduced && moving && seat<.1 ? Math.abs(Math.sin(now/180))*.65*scale : 0;
      const flying=!reduced && (hovering || moving || Math.hypot(tx-x,ty-y)>.7*scale);
      return {x,y:y-hop,seat,hovering,flap:flying && Math.floor(now/160)%2===0};
    }
  };}
  return Object.freeze({create});
});
