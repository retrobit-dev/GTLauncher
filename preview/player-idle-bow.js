// Cosmetic idle bow: no world data is written. Times are monotonic milliseconds.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.PlayerIdleBow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const DELAY=3000, DURATION=2000, INTERVAL=3000;
  const smooth=t=>{t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);};
  function amount(elapsed){if(elapsed<DELAY)return 0;const t=(elapsed-DELAY)%INTERVAL;if(t>=DURATION)return 0;if(t<500)return smooth(t/500);if(t<1500)return 1;return 1-smooth((t-1500)/500);}
  function create(){let key=null,since=0;return {update(now,nextKey,enabled){if(!enabled||!nextKey){key=null;since=now;return 0;}if(key!==nextKey||now<since){key=nextKey;since=now;return 0;}return amount(now-since);},reset(){key=null;}};}
  // Whole-frame poses avoid distorted hands or limbs; 0=standing, 1=transition, 2=hands on knees.
  function frame(value){return value<.2?0:value<.75?1:2;}
  return Object.freeze({DELAY,DURATION,INTERVAL,amount,create,frame});
});
