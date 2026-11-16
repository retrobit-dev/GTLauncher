(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const phase = $('hud-phase'), season = $('hud-season'), battery = $('hud-battery');
  if (!phase || !season || !battery) return;
  const pad = n => String(n).padStart(2,'0');
  let seasonKey = '', lastFetch = 0, requestId = 0;
  function showSeason(value) {
    const labels={wet:'Perkiraan musim hujan',dry:'Perkiraan musim kemarau',unknown:'Musim belum diketahui'};
    season.src=`assets/hud/season_${value}.png`;
    season.alt=labels[value];season.title=labels[value];
  }
  async function refreshSeason(loc,now) {
    if (!loc || !Number.isFinite(loc.latitude) || !Number.isFinite(loc.longitude)) return;
    const key=`${loc.latitude.toFixed(3)},${loc.longitude.toFixed(3)}`;
    if (key===seasonKey && now.getTime()-lastFetch<12*60*60*1000) return;
    if(key!==seasonKey) showSeason('unknown');
    seasonKey=key;lastFetch=now.getTime();const id=++requestId;
    const today=Date.UTC(now.getUTCFullYear(),now.getUTCMonth(),now.getUTCDate());
    const date=offset=>new Date(today+offset*86400000).toISOString().slice(0,10);
    const params=new URLSearchParams({latitude:loc.latitude.toFixed(5),longitude:loc.longitude.toFixed(5),start_date:date(-550),end_date:date(-7),daily:'precipitation_sum',timezone:'auto'});
    try {
      const response=await fetch(`https://archive-api.open-meteo.com/v1/archive?${params}`);
      if(!response.ok)throw Error('Rainfall unavailable');
      const result=window.SeasonEstimate.classify(await response.json(),now);
      if(id===requestId)showSeason(result);
    } catch (_) { if(id===requestId)showSeason('unknown'); }
  }
  function updateClock() {
    const now=new Date();$('hud-clock').textContent=`${pad(now.getHours())}:${pad(now.getMinutes())}`;
    $('hud-month').textContent=`${pad(now.getMonth()+1)}/${String(now.getFullYear()).slice(-2)}`;
    $('hud-day').textContent=String(now.getDate());
    $('hud-weekday').textContent=new Intl.DateTimeFormat('id-ID',{weekday:'short'}).format(now).toUpperCase();
    const altitude=window.previewHudSunAltitude?.(now.getTime());
    const isNight=Number.isFinite(altitude) && altitude<=0;
    phase.hidden=!Number.isFinite(altitude);
    phase.src=`assets/hud-${isNight?'night':'day'}.svg`;
    phase.alt=Number.isFinite(altitude)?(isNight?'Malam':'Siang'):'Waktu siang atau malam belum diketahui';
    phase.title=phase.alt;
    refreshSeason(window.previewHudLocation?.(),now);
  }
  updateClock();window.setInterval(updateClock,60000);
  if(typeof navigator.getBattery==='function')navigator.getBattery().then(info=>{
    function update(){const value=Number.isFinite(info.level)?`${Math.round(info.level*100)}%`:'--%';battery.textContent=value;battery.setAttribute('aria-label',value==='--%'?'Status baterai tidak tersedia':`Baterai ${value}${info.charging?', mengisi daya':''}`);}
    info.addEventListener('levelchange',update);info.addEventListener('chargingchange',update);update();
  }).catch(()=>{});
  const dialog=$('dock-dialog'),title=$('dock-dialog-title'),message=$('dock-message');
  const info={phone:['Aplikasi','Daftar aplikasi perangkat hanya tersedia di Android; browser tidak dapat membacanya.'],bag:['Tas','Tas preview belum dipulihkan. Data inventaris tidak diubah.'],settings:['Pengaturan','Pengaturan dock belum tersedia.']};
  for(const button of document.querySelectorAll('[data-dock]'))button.addEventListener('click',()=>{const entry=info[button.dataset.dock];if(!entry)return;title.textContent=entry[0];message.textContent=entry[1];if(!dialog.open)dialog.showModal();});
  $('dock-close').addEventListener('click',()=>dialog.close());
})();
