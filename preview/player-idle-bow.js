// Cosmetic idle bow: no world data is written. Times are monotonic milliseconds.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.PlayerIdleBow=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const DELAY=3000, DURATION=2000, INTERVAL=3000;
  const smooth=t=>{t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);};
  function amount(elapsed){if(elapsed<DELAY)return 0;const t=(elapsed-DELAY)%INTERVAL;if(t>=DURATION)return 0;if(t<500)return smooth(t/500);if(t<1500)return 1;return 1-smooth((t-1500)/500);}
  function create(){let key=null,since=0;return {update(now,nextKey,enabled){if(!enabled||!nextKey){key=null;since=now;return 0;}if(key!==nextKey||now<since){key=nextKey;since=now;return 0;}return amount(now-since);},reset(){key=null;}};}
  function draw(ctx,sprite,width,height,bow){
    const hip=Math.round(height*.68), sx=1-.20*bow, shift=width*.10*bow, sy=1-.20*bow;
    ctx.clearRect(0,0,width,height);ctx.imageSmoothingEnabled=false;
    ctx.drawImage(sprite,0,hip,width,height-hip,0,hip,width,height-hip);
    ctx.save();ctx.transform(sx,0,-shift/hip,sy,width*(1-sx)/2+shift,hip*(1-sy));
    ctx.drawImage(sprite,0,0,width,hip,0,0,width,hip);ctx.restore();
  }
  return Object.freeze({DELAY,DURATION,INTERVAL,amount,create,draw});
});
