// Preview gameplay: route a visible character without changing world data until arrival.
(() => {
  'use strict';
  function columnLabel(column) { let n=column+1, label=''; while(n>0) { n--; label=String.fromCharCode(65+n%26)+label; n=Math.floor(n/26); } return label; }
  const stage=document.querySelector('.preview-stage'),canvas=document.getElementById('sky-canvas');
  const status=document.getElementById('gameplay-movement-status'),scene=window.sceneEditing,worlds=window.worldManager,path=window.GameplayPath;
  if(!stage||!canvas||!status||!scene?.setGameplayDraft||!worlds?.updateCharacter||!path)return;
  let gesture=null, walk=null, queuedTarget=null, generation=0, committing=false, movingHouse=null, selectedHouse=null;
  const objectDialog=document.getElementById('hand-object-dialog');
  function clearObjectSelection(){movingHouse=null;selectedHouse=null;if(objectDialog?.open)objectDialog.close();}
  if(objectDialog){
    document.getElementById('hand-object-move').addEventListener('click',()=>{movingHouse=selectedHouse;objectDialog.close();tell('Ketuk tile untuk sudut kiri atas rumah 3×3.');});
    document.getElementById('hand-object-cancel').addEventListener('click',()=>{clearObjectSelection();tell('Pemindahan dibatalkan.');});
    document.getElementById('hand-object-close').addEventListener('click',()=>{clearObjectSelection();tell('Pemindahan dibatalkan.');});
    objectDialog.addEventListener('cancel',()=>{movingHouse=null;selectedHouse=null;});
  }
  const reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function tell(text){status.textContent=text;status.hidden=!text;}
  function cancel() { generation++; walk=null;queuedTarget=null;scene.setGameplayRoute?.(null);scene.setGameplayDraft(null); }
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
      const next=queuedTarget,walkMessage=walk.message; cancel();
      if(!ok){tell('Posisi tidak tersimpan; periksa penyimpanan browser.');return;}
      if(next){move(next);return;}
      tell(walkMessage || `Tiba di ${columnLabel(end.column)}${end.row+1}.`);return;
    }
    if(walk.routeIndex!==index){walk.routeIndex=index;scene.setGameplayRoute?.(walk.route,index);}
    const a=walk.route[index-1],b=walk.route[index],t=Math.max(0,Math.min(1,elapsed/walk.durations[index-1]));
    scene.setGameplayDraft({...walk.character,column:a.column+(b.column-a.column)*t,row:a.row+(b.row-a.row)*t,facing:direction(a,b),walkPhase:(index-1+t)*Math.PI*2});
    window.requestAnimationFrame(next=>step(next,token));
  }
  function direction(a,b){return b.column>a.column?'E':b.column<a.column?'W':b.row>a.row?'S':'N';}
  function move(target) {
    if(walk){queuedTarget=target;tell('Tujuan berikutnya disimpan; karakter sedang berjalan.');return;}
    const world=worlds.getActive(), character=worlds.getCharacter();
    if(world.id==='base'||!character.visible){tell('Buka atau buat world dengan karakter melalui tombol World.');return;}
    const toolSelected=!!document.querySelector('.home-dock [data-dock][aria-pressed="true"]');
    const house=worlds.getHouse(), houseTarget=house?.visible && target.column>=house.column && target.column<house.column+3 && target.row>=house.row && target.row<house.row+3;
    const route=houseTarget?path.findBeside(world,character,house):path.find(world,character,target,house);
    if(!route){scene.setGameplayRoute?.(null);tell(houseTarget?'Jalan menuju rumah terhalang.':'Tile tujuan tidak dapat dicapai.');return;}
    if(toolSelected && houseTarget && route.length===1){
      const adjacent={column:Math.max(house.column,Math.min(house.column+2,character.column)),row:Math.max(house.row,Math.min(house.row+2,character.row))};
      const facing=direction(character,adjacent);
      if(facing!==character.facing){committing=true;let ok=false;try{ok=worlds.updateCharacter(world.id,{...character,facing});}finally{committing=false;}tell(ok?'Arah hadap berubah. Ketuk objek lagi untuk memilihnya.':'Arah hadap tidak tersimpan.');return;}
    }
    if(toolSelected && route.length>1){
      const facing=direction(route[0],route[1]);
      if(character.facing!==facing){
        committing=true;let ok=false;try{ok=worlds.updateCharacter(world.id,{...character,facing});}finally{committing=false;}
        scene.setGameplayRoute?.(ok?route:null);tell(ok?'Arah hadap berubah. Ketuk tujuan lagi untuk bergerak.':'Arah hadap tidak tersimpan.');return;
      }
    }
    const end=route[route.length-1], completion=houseTarget?'Di dekat rumah. Interaksi rumah belum tersedia.':'';
    cancel();
    if(route.length===1){tell(completion || 'Karakter sudah berada di tile ini.');return;}
    tell(houseTarget?'Berjalan mendekati rumah…':`Berjalan ke ${columnLabel(end.column)}${end.row+1}…`);
    const total=route.length-1;
    if(reduced){
      const previous=route[total-1];committing=true;let ok=false;
      try{ok=worlds.updateCharacter(world.id,{...character,...route[total],facing:direction(previous,route[total])});}finally{committing=false;}
      tell(ok?(completion || `Tiba di ${columnLabel(end.column)}${end.row+1}.`):'Posisi tidak tersimpan; periksa penyimpanan browser.');return;
    }
    scene.setGameplayRoute?.(route,1);
    walk={id:world.id,character,route,message:completion,routeIndex:1,started:null,durations:route.slice(1).map(()=>650)};
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
    const local=scene.toLocal(event.clientX,event.clientY), house=worlds.getHouse(), character=worlds.getCharacter();
    if(movingHouse){
      const target=scene.tileAt(local);if(!target){tell('Pilih tile di dalam world.');return;}
      committing=true;let ok=false;try{ok=worlds.moveHouse?.(movingHouse.id,movingHouse.house,target.column,target.row)===true;}finally{committing=false;}
      if(ok){movingHouse=null;selectedHouse=null;tell('Rumah berhasil dipindahkan.');}
      else tell('Rumah tidak dapat ditempatkan di sini. Periksa tapak, air, karakter, atau perubahan world.');return;
    }
    const selected=document.querySelector('.home-dock [data-dock][aria-pressed="true"]')?.dataset.dock;
    const offset={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[character.facing]||[0,0];
    const front={column:character.column+offset[0],row:character.row+offset[1]};
    const hitHouse=scene.houseAt?.(local,house), tile=scene.tileAt(local);
    const frontHouse=house?.visible && front.column>=house.column && front.column<house.column+3 && front.row>=house.row && front.row<house.row+3;
    const hitFront=tile && tile.column===front.column && tile.row===front.row;
    const hitCharacter=!hitFront && !hitHouse && scene.hit?.(local,character);
    if(selected && (hitCharacter || ((hitFront || hitHouse) && selected==='hand' && frontHouse))) {
      scene.setGameplayRoute?.(null);
      if(selected==='hand') {
        if(frontHouse && objectDialog){cancel();selectedHouse={id:worlds.getActive().id,house:{...house}};objectDialog.showModal();tell('Rumah dipilih.');}
        else tell(frontHouse?'Rumah dipilih. Interaksi rumah belum tersedia.':'Tidak ada objek di depan karakter.');
      }
      else if(selected==='hoe')tell('Blok tanah pertanian belum tersedia; tidak ada tile yang diubah.');
      else tell('Fungsi alat ini belum tersedia.');
      return;
    }
    // House artwork is an interaction target only from its facing side; otherwise it is a walking destination.
    const target=hitHouse?{column:Math.max(house.column,Math.min(house.column+2,character.column)),row:Math.max(house.row,Math.min(house.row+2,character.row))}:tile;
    if(target)move(target);
  });
  canvas.addEventListener('pointercancel',()=>{gesture=null;});
  document.getElementById('mode-map-editor').addEventListener('click',()=>{gesture=null;clearObjectSelection();cancel();tell('');});
  window.addEventListener('gameplaytoolchange',()=>{gesture=null;clearObjectSelection();cancel();tell('');});
  window.addEventListener('worldchange',()=>{if(!committing){gesture=null;clearObjectSelection();cancel();tell('');}});
})();
