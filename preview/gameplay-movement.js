// Preview gameplay: route a visible character without changing world data until arrival.
(() => {
  'use strict';
  const stage=document.querySelector('.preview-stage'),canvas=document.getElementById('sky-canvas');
  const status=document.getElementById('gameplay-movement-status'),scene=window.sceneEditing,worlds=window.worldManager,path=window.GameplayPath;
  if(!stage||!canvas||!status||!scene?.setGameplayDraft||!worlds?.updateCharacter||!path)return;
  let gesture=null, walk=null, queuedTarget=null, generation=0, committing=false;
  const reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function tell(text){status.textContent=text;status.hidden=!text;}
  function cancel() { generation++; walk=null;queuedTarget=null;scene.setGameplayDraft(null); }
  function step(timestamp,token) {
    if(token!==generation||!walk)return;
    if(stage.dataset.mode!=='gameplay'||worlds.getActive().id!==walk.id){cancel();return;}
    if(walk.started===null) walk.started=timestamp;
    let elapsed=timestamp-walk.started,index=1;
    while(index<walk.route.length && elapsed>=walk.durations[index-1]){elapsed-=walk.durations[index-1];index++;}
    if(index>=walk.route.length){
      const end=walk.route[walk.route.length-1],previous=walk.route[walk.route.length-2];
      const facing=direction(previous,end),character={...walk.character,...end,facing};
      committing=true;let ok=false;try{ok=worlds.updateCharacter(walk.id,character);}finally{committing=false;}
      const next=queuedTarget; cancel();
      if(!ok){tell('Posisi tidak tersimpan; periksa penyimpanan browser.');return;}
      if(next){move(next);return;}
      tell(`Tiba di ${'ABCDEFGHIJKLMNOPQRSTUVWXY'[end.column]}${end.row+1}.`);return;
    }
    const a=walk.route[index-1],b=walk.route[index],t=Math.max(0,Math.min(1,elapsed/walk.durations[index-1]));
    scene.setGameplayDraft({...walk.character,column:a.column+(b.column-a.column)*t,row:a.row+(b.row-a.row)*t,facing:direction(a,b)});
    window.requestAnimationFrame(next=>step(next,token));
  }
  function direction(a,b){return b.column>a.column?'E':b.column<a.column?'W':b.row>a.row?'S':'N';}
  function move(target) {
    if(walk){queuedTarget=target;tell('Tujuan berikutnya disimpan; karakter sedang berjalan.');return;}
    const world=worlds.getActive(), character=worlds.getCharacter();
    if(world.id==='base'||!character.visible){tell('Buka atau buat world dengan karakter melalui tombol World.');return;}
    const route=path.find(world,character,target,worlds.getHouse());
    if(!route){tell('Tile tujuan tidak dapat dicapai.');return;}
    cancel();
    if(route.length===1){tell('Karakter sudah berada di tile ini.');return;}
    tell(`Berjalan ke ${'ABCDEFGHIJKLMNOPQRSTUVWXY'[target.column]}${target.row+1}…`);
    const total=route.length-1;
    if(reduced){
      const previous=route[total-1];committing=true;let ok=false;
      try{ok=worlds.updateCharacter(world.id,{...character,...route[total],facing:direction(previous,route[total])});}finally{committing=false;}
      tell(ok?`Tiba di ${'ABCDEFGHIJKLMNOPQRSTUVWXY'[target.column]}${target.row+1}.`:'Posisi tidak tersimpan; periksa penyimpanan browser.');return;
    }
    walk={id:world.id,character,route,started:null,durations:route.slice(1).map((_,i)=>total>3&&i>0&&i<total-1?180:300)};
    const token=++generation;window.requestAnimationFrame(t=>step(t,token));
  }
  canvas.addEventListener('pointerdown',event=>{
    if(stage.dataset.mode!=='gameplay'||(event.pointerType==='mouse'&&event.button!==0)||gesture)return;
    gesture={id:event.pointerId,x:event.clientX,y:event.clientY};
  });
  canvas.addEventListener('pointerup',event=>{
    if(!gesture||gesture.id!==event.pointerId)return;
    const start=gesture;gesture=null;
    if(stage.dataset.mode!=='gameplay'||Math.hypot(event.clientX-start.x,event.clientY-start.y)>8)return;
    const target=scene.tileAt(scene.toLocal(event.clientX,event.clientY));if(target)move(target);
  });
  canvas.addEventListener('pointercancel',()=>{gesture=null;});
  document.getElementById('mode-map-editor').addEventListener('click',()=>{gesture=null;cancel();tell('');});
  window.addEventListener('worldchange',()=>{if(!committing){gesture=null;cancel();tell('');}});
})();
