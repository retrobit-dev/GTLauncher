// Preview-only inventory and pickup ledger share one atomic localStorage write.
(() => {
  'use strict';
  const KEY='gtlauncher.orange-drops.v1', PERIOD=5*60*1000, FALL_TIME=1600;
  let cachedRaw, cachedState;
  function read() {
    const raw=localStorage.getItem(KEY);
    if(raw===cachedRaw && cachedState) return cachedState;
    const state=raw===null?{version:1,orange:0,collected:{}}:JSON.parse(raw);
    if(!state || state.version!==1 || Object.keys(state).some(k=>!['version','orange','collected'].includes(k)) ||
      !Number.isSafeInteger(state.orange) || state.orange<0 || state.orange>1000000 ||
      !state.collected || Array.isArray(state.collected) || typeof state.collected!=='object' ||
      Object.entries(state.collected).some(([k,v])=>k.length>300 || !Number.isSafeInteger(v) || v<0)) throw Error('Data tas jeruk tidak valid; data tidak diubah.');
    cachedRaw=raw;cachedState=state;return state;
  }
  const key=(world,tree)=>`${encodeURIComponent(world.id)}:${tree.column},${tree.row}`;
  const cycle=now=>Math.floor(now/PERIOD);
  function available(world,tree,now=Date.now()) {
    if(!world || world.id==='base' || !tree || tree.kind!==undefined || !Number.isFinite(now) || now<0)return false;
    try {const history=read().collected,k=key(world,tree),last=Object.hasOwn(history,k)?history[k]:-1;return last<cycle(now);} catch (_) {return false;}
  }
  function falling(now=Date.now(),reduced=false) {return reduced?1:Math.min(1,(now%PERIOD)/FALL_TIME);}
  function count() {try{return read().orange;}catch(_){return null;}}
  async function collect(worldId,tree,now=Date.now()) {
    if(!navigator.locks?.request) return {ok:false,message:'Browser belum mendukung penguncian aman untuk mengambil jeruk.'};
    try {return await navigator.locks.request('gtlauncher.orange-drops.v1',()=>{
      const world=window.worldManager?.getActive();
      if(!world || world.id!==worldId || !(world.orangeTrees||[]).some(t=>t.kind===undefined && t.column===tree.column && t.row===tree.row))return {ok:false,message:'Pohon sudah berubah; jeruk tidak diambil.'};
      // Do not rely solely on another tab's asynchronous storage event.
      const stored=JSON.parse(localStorage.getItem('gtlauncher.worlds.v1') || 'null');
      const saved=stored?.worlds?.find(w=>w.id===worldId);
      if(stored?.activeId!==worldId || !saved?.orangeTrees?.some(t=>t.kind===undefined && t.column===tree.column && t.row===tree.row))return {ok:false,message:'Pohon sudah berubah; jeruk tidak diambil.'};
      // Re-read after acquiring the cross-tab lock and re-check the current cycle.
      const current=read(),actual=Date.now(),period=cycle(actual);
      if(cycle(now)!==period || !available(world,tree,actual))return {ok:false,message:'Jeruk sudah diambil atau periodenya berubah.'};
      if(current.orange>=1000000)return {ok:false,message:'Tas jeruk penuh.'};
      const next={version:1,orange:current.orange+1,collected:{...current.collected,[key(world,tree)]:period}};
      const raw=JSON.stringify(next);localStorage.setItem(KEY,raw);cachedRaw=raw;cachedState=next;
      window.dispatchEvent(new Event('orangefruitupdate'));return {ok:true,message:'Jeruk masuk ke tas.'};
    });} catch (_) {return {ok:false,message:'Jeruk tidak tersimpan; periksa penyimpanan browser.'};}
  }
  window.addEventListener('storage',e=>{if(e.key===KEY){cachedRaw=undefined;cachedState=null;window.dispatchEvent(new Event('orangefruitupdate'));}});
  let lastPeriod=cycle(Date.now());setInterval(()=>{const next=cycle(Date.now());if(next!==lastPeriod){lastPeriod=next;window.dispatchEvent(new Event('orangefruitupdate'));}},1000);
  window.OrangeDrops=Object.freeze({available,falling,count,collect,period:PERIOD});
})();
