// Shared cloud-shadow rules, matching the Android decorative layout.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.CloudLayout=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  function count(cover,code){const value=Math.max(0,Math.min(100,cover));let n=value>=82?8:value>=58?6:value>=32?4:3;if(code===0||code===1)return Math.min(n,4);if(code===3||code===45||code===48||code>=51)n=Math.max(n,5);return n;}
  function cellSize(w,h,cols,rows){return 4*Math.min(w/cols,h/rows);}
  function wet(cover,code){return cover>=65||code===3||code===45||code===48||code>=51;}
  function fade(seconds,animated){return animated?.6+.4*(1+Math.cos(2*Math.PI*seconds/24))/2:1;}
  return Object.freeze({count,cellSize,wet,fade});
});
