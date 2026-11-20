// Independent preview bag + harvest ledger: one atomic write, no modification of the orange bag.
(() => {
  'use strict';
  const KEY='gtlauncher.bush-harvest.v1', PERIOD=300000, READY=180000;
  let cachedRaw,cachedState;
  const type=t=>t?.kind==='bush' && ['flower','berry'].includes(t.variant)?t.variant:null;
  const key=(world,t)=>`${world.id}:${t.plantId || `legacy-${t.variant}`}`;
  function state() {
    const raw=localStorage.getItem(KEY);if(raw===cachedRaw && cachedState)return cachedState;
    const s=raw===null?{version:1,flower:0,berry:0,harvests:{}}:JSON.parse(raw);
    if(!s || s.version!==1 || Object.keys(s).some(k=>!['version','flower','berry','harvests'].includes(k)) || !['flower','berry'].every(k=>Number.isSafeInteger(s[k]) && s[k]>=0 && s[k]<=1000000) || !s.harvests || Array.isArray(s.harvests) || typeof s.harvests!=='object' || Object.entries(s.harvests).some(([k,v])=>k.length>220 || !Number.isSafeInteger(v) || v<0))throw Error('Data panen tidak valid');
    cachedRaw=raw;cachedState=s;return s;
  }
  function cycle(t,now) {const start=t.plantedAt??0;return now>=start?Math.floor((now-start)/PERIOD):-1;}
  function phase(t,now) {const n=cycle(t,now);return n>=0 && (now-(t.plantedAt??0))%PERIOD>=READY;}
  function ready(world,t,now=Date.now()) {
    if(!world || world.id==='base' || !type(t) || !Number.isSafeInteger(now) || now<0 || !phase(t,now))return false;
    try {const last=state().harvests[key(world,t)];return last===undefined || last<cycle(t,now);} catch(_){return false;}
  }
  function counts(){try{const s=state();return {flower:s.flower,berry:s.berry};}catch(_){return null;}}
  async function collect(worldId,plant) {
    if(!navigator.locks?.request)return {ok:false,message:'Browser belum mendukung penguncian aman untuk panen.'};
    try{return await navigator.locks.request(KEY,()=>{
      const saved=JSON.parse(localStorage.getItem('gtlauncher.worlds.v1')||'null'),world=saved?.worlds?.find(w=>w.id===worldId);
      const actual=world?.orangeTrees?.find(t=>plant.plantId?t.plantId===plant.plantId:JSON.stringify(t)===JSON.stringify(plant));
      if(saved?.activeId!==worldId || !actual || !type(actual))return {ok:false,message:'Tanaman telah berubah; panen dibatalkan.'};
      const c=world.characterColumn,r=world.characterRow,n=actual.size||2;
      const dx=Math.max(actual.column-c,0,c-(actual.column+n-1)),dy=Math.max(actual.row-r,0,r-(actual.row+n-1));
      if(!world.showCharacter || dx+dy!==1)return {ok:false,message:'Dekati semak untuk memanen.'};
      const now=Date.now(),s=state(),kind=type(actual);if(!ready(world,actual,now))return {ok:false,message:'Belum siap atau sudah dipanen pada siklus ini.'};
      if(s[kind]>=1000000)return {ok:false,message:'Tas penuh; panen tidak diambil.'};
      const next={...s,[kind]:s[kind]+1,harvests:{...s.harvests,[key(world,actual)]:cycle(actual,now)}};
      const raw=JSON.stringify(next);localStorage.setItem(KEY,raw);cachedRaw=raw;cachedState=next;
      window.dispatchEvent(new Event('bushharvestchange'));return {ok:true,message:`${kind==='flower'?'Melati':'Murbei'} masuk ke tas.`};
    });}catch(_){return {ok:false,message:'Panen tidak tersimpan; periksa data dan ruang penyimpanan.'};}
  }
  window.addEventListener('storage',e=>{if(e.key===KEY){cachedRaw=undefined;cachedState=null;window.dispatchEvent(new Event('bushharvestchange'));};});
  setInterval(()=>window.dispatchEvent(new Event('bushharvestchange')),1000);
  window.BushHarvest=Object.freeze({ready,counts,collect,cycle,phase,PERIOD,READY});
})();
