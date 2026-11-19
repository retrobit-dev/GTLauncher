// Cosmetic idle bow: no world data is written. Times are monotonic milliseconds.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.PlayerIdleBow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const DELAY=8000, DURATION=2000, INTERVAL=8000, SIT_DELAY=30000, SIT_TRANSITION=1800;
  const smooth=t=>{t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);};
  function amount(elapsed){if(elapsed<DELAY)return 0;const t=(elapsed-DELAY)%INTERVAL;if(t>=DURATION)return 0;if(t<500)return smooth(t/500);if(t<1500)return 1;return 1-smooth((t-1500)/500);}
  function create(){let key=null,since=0,elapsed=0;return {update(now,nextKey,enabled,allowSitting=true){if(!enabled||!nextKey){key=null;since=now;elapsed=0;return 0;}if(key!==nextKey||now<since){key=nextKey;since=now;elapsed=0;return 0;}elapsed=now-since;return allowSitting && elapsed>=SIT_DELAY?0:amount(elapsed);},sitting(){return key?smooth((elapsed-SIT_DELAY)/SIT_TRANSITION):0;},reset(){key=null;elapsed=0;}};}
  // Last two seated frames have a consistent hip line at sprite y=700/800.
  function seatGroundWeight(sitting){return smooth(sitting*3-1);}
  // Whole-frame poses avoid distorted hands or limbs; 0=standing, 1=transition, 2=hands on knees.
  function frame(value){return value<.2?0:value<.75?1:2;}
  return Object.freeze({DELAY,DURATION,INTERVAL,SIT_DELAY,SIT_TRANSITION,amount,create,frame,seatGroundWeight});
});
