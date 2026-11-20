// Gentle gait-linked movement of the top hair only; values are in scene units.
(function(root,factory){
  if(typeof module==='object' && module.exports) module.exports=factory();
  else root.CharacterHairMotion=factory();
})(typeof globalThis!=='undefined'?globalThis:this,function(){
  'use strict';
  const HAIR_END=.16;
  function offset(y, gait, runBlend, scale, enabled=true){
    if(!enabled || ![y,gait,runBlend,scale].every(Number.isFinite) || scale<=0 || y>=HAIR_END) return 0;
    const falloff=Math.max(0,Math.min(1,1-Math.max(0,y)/HAIR_END));
    const running=Math.max(0,Math.min(1,runBlend));
    return Math.max(-1,Math.min(1,gait))*(.2+.12*running)*scale*falloff*falloff;
  }
  return Object.freeze({HAIR_END,offset});
});
