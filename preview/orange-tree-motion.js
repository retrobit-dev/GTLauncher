// Cosmetic anchored wind motion. Fruit and leaves share the same tree artwork.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.OrangeTreeMotion=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function sway(enabled,speed,direction,heading,time,column,row) {
    if(!enabled || !Number.isFinite(speed) || speed<=0 || !Number.isFinite(time))return 0;
    const bearing=((Number.isFinite(direction)?direction:90)+180-(Number.isFinite(heading)?heading:0))*Math.PI/180;
    const projection=Math.sin(bearing)+.25*Math.cos(bearing);
    const sign=projection<0?-1:1;
    const amplitude=Math.min(2.8,speed*.26);
    const phase=column*1.73+row*2.31;
    return sign*amplitude*(.16+.84*Math.sin(time*.72+phase));
  }
  // The ground stays fixed; displacement grows linearly toward the top.
  function offset(sway,heightAboveGround,totalHeight) {return sway*heightAboveGround/totalHeight;}
  return Object.freeze({sway,offset});
});
