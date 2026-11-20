(() => {
  'use strict';
  function columnLabel(column) { let n=column+1, label=''; while(n>0) { n--; label=String.fromCharCode(65+n%26)+label; n=Math.floor(n/26); } return label; }
  const $ = id => document.getElementById(id);
  const dialog = $('object-dialog'), form = $('object-form'), canvas = $('sky-canvas');
  const editButton = $('object-edit-button'), detailsButton = $('object-details-button');
  const selectButton = $('object-select-button'), selectHouse = $('object-select-house');
  const undoButton = $('object-undo'), redoButton = $('object-redo');
  const rotateControls = $('rotate-controls'), rotateLeft = $('rotate-left'), rotateRight = $('rotate-right');
  const directions = ['N', 'E', 'S', 'W'];
  const directionNames = {N: 'Utara', E: 'Timur', S: 'Selatan', W: 'Barat'};
  const status = $('edit-status');
  if (!dialog || !form || !canvas || !window.worldManager) return;
  const libraryDialog = $('object-library-dialog');
  const libraryList = $('object-library-list');
  const catalogTabs=[$('catalog-tab-buildings'),$('catalog-tab-plants')];
  function selectCatalogTab(index,focus=false) {
    catalogTabs.forEach((tab,i)=>{const active=i===index;tab.setAttribute('aria-selected',String(active));tab.tabIndex=active?0:-1;$(tab.getAttribute('aria-controls')).hidden=!active;});
    if(focus)catalogTabs[index].focus();
  }
  catalogTabs.forEach((tab,index)=>{
    tab.addEventListener('click',()=>selectCatalogTab(index));
    tab.addEventListener('keydown',event=>{if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;event.preventDefault();selectCatalogTab(event.key==='Home'?0:event.key==='End'?catalogTabs.length-1:(index+1)%catalogTabs.length,true);});
  });
  let catalogReplacement=null, catalogSelection=[];
  const objectSets=world=>({trees:(world.orangeTrees||[]).map(o=>({...o})),fences:(world.fences||[]).map(o=>({...o})),bridges:(world.bridges||[]).map(o=>({...o}))});
  function renderLibrary() {
    const world = window.worldManager.getActive();
    $('object-library-world').textContent = `World: ${world.name}`;
    libraryList.replaceChildren();
    const houseButton=$('catalog-place-house'), exists=Boolean(window.worldManager.getHouse());
    houseButton.disabled=world.id==='base' || exists || !Array.isArray(world.tiles);
    $('catalog-place-orange').disabled=world.id==='base' || !Array.isArray(world.tiles);
    $('catalog-place-conifer').disabled=world.id==='base' || !Array.isArray(world.tiles);
    $('catalog-place-conifer-small').disabled=world.id==='base' || !Array.isArray(world.tiles);
    for(const suffix of ['', '-large','-flower','-flower-large','-berry','-berry-large']) $('catalog-place-bush'+suffix).disabled=world.id==='base' || !Array.isArray(world.tiles);
    for(const kind of ['wood','stone']) { $('catalog-place-fence-'+kind).disabled=world.id==='base' || !Array.isArray(world.tiles); for(const suffix of ['', '-4','-5']) $('catalog-place-bridge-'+kind+suffix).disabled=world.id==='base' || !Array.isArray(world.tiles); }
    $('catalog-place-wild-grass').disabled=world.id==='base' || !Array.isArray(world.tiles);
    $('catalog-house-status').textContent=world.id==='base' ? 'Base World terkunci. Buka world buatan untuk menempatkan rumah.' : exists ? 'World ini sudah memiliki rumah player. Pindahkan rumah yang ada bila diperlukan.' : !Array.isArray(world.tiles) ? 'World lama ini tidak mendukung penempatan rumah.' : 'Pilih tombol, lalu ketuk sudut pertama tapak pada peta.';
    $('catalog-bulk-replace').disabled=!editing || !catalogSelection.length; $('catalog-bulk-delete').disabled=!editing || !catalogSelection.length;
    $('catalog-selection-count').textContent=`${catalogSelection.length} dipilih`;
    $('catalog-replace-status').hidden=!catalogReplacement;$('catalog-replace-cancel').hidden=!catalogReplacement;
    $('catalog-replace-status').textContent=catalogReplacement?`Ganti ${catalogReplacement.label}: pilih objek pengganti di katalog. Posisi awal tetap; tapak diperiksa sebelum disimpan.`:'';
    const character=window.worldManager.getCharacter(),house=window.worldManager.getHouse?.();
    function row(label,image,key,index,object,protectedObject=false) {
      const node=document.createElement('div');node.className='world-card catalog-placed-row';
      const icon=document.createElement('img');icon.className='catalog-placed-icon';icon.src=image;icon.alt='';node.append(icon);
      const detail=document.createElement('div');detail.className='world-card-details';detail.textContent=label;node.append(detail);
      if(protectedObject){const note=document.createElement('small');note.textContent='Tidak melalui daftar';node.append(note);}
      else {
        const check=document.createElement('input');check.type='checkbox';check.className='catalog-check';check.setAttribute('aria-label',`Pilih ${label}`);check.disabled=!editing || world.id==='base';check.checked=catalogSelection.some(s=>s.key===key&&s.index===index&&JSON.stringify(s.object)===JSON.stringify(object));
        check.addEventListener('change',()=>{catalogSelection=catalogSelection.filter(s=>!(s.key===key&&s.index===index));if(check.checked)catalogSelection.push({key,index,object:{...object},label});$('catalog-bulk-replace').disabled=!catalogSelection.length;$('catalog-bulk-delete').disabled=!catalogSelection.length;$('catalog-selection-count').textContent=`${catalogSelection.length} dipilih`;});node.prepend(check);
        const actions=document.createElement('div');actions.className='catalog-placed-actions';
        for(const [action,path] of [['replace','M4 8h13l-3-3m3 3-3 3M20 16H7l3-3m-3 3 3 3'],['delete','M4 6h16M9 6V4h6v2m3 0-1 14H7L6 6m4 4v6m4-6v6']]) {
          const button=document.createElement('button');button.type='button';button.className='catalog-action';button.dataset.action=action;button.setAttribute('aria-label',`${action==='replace'?'Ganti':'Hapus'} ${label}`);button.title=action==='replace'?'Ganti objek':'Hapus objek';button.disabled=!editing || world.id==='base';
          const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 24 24');svg.setAttribute('aria-hidden','true');const line=document.createElementNS('http://www.w3.org/2000/svg','path');line.setAttribute('d',path);svg.append(line);button.append(svg);
          button.addEventListener('click',()=>{if(action==='replace'){deactivateTerrain();syncControls();catalogReplacement={worldId:world.id,key,index,object:{...object},expected:objectSets(world),label};renderLibrary();selectCatalogTab(0);$('object-library-dialog').scrollTop=0;}
            else if(window.confirm(`Hapus ${label}? Data panen terkait tidak dipindahkan.`)){const before=objectSets(window.worldManager.getActive());if(world.id!==window.worldManager.getActive().id || JSON.stringify(before[key][index])!==JSON.stringify(object)){tell('Objek sudah berubah; data tidak diubah.');return;}const next={trees:before.trees.map(o=>({...o})),fences:before.fences.map(o=>({...o})),bridges:before.bridges.map(o=>({...o}))};next[key].splice(index,1);if(saveCatalogObjects(before,next)){catalogReplacement=null;renderLibrary();tell('EDITING · Objek dihapus.');}else tell('EDITING · Objek tidak dapat dihapus; data tidak diubah.');}});
          actions.append(button);
        }node.append(actions);
      }libraryList.append(node);
    }
    if(character.visible){const snapshot=window.sceneEditing?.characterCatalogIcon?.();row(`Karakter · ${columnLabel(character.column)}${character.row+1}`,snapshot || `assets/main-character${['N','W'].includes(character.facing)?'-back':''}.png?v=short-stocky-1`,null,0,null,true);if(!snapshot && ['W','E'].includes(character.facing))libraryList.lastElementChild.querySelector('img').style.transform='scaleX(-1)';}
    if(house)row(`Rumah player · ${columnLabel(house.column)}${house.row+1}${house.visible?'':' (tersembunyi)'}`,'assets/simple-house-aligned.png',null,0,null,true);
    (world.orangeTrees||[]).forEach((t,i)=>{const label=t.kind==='bush'?(t.variant==='flower'?'Semak melati':t.variant==='berry'?'Semak murbei':'Semak'):t.kind==='wild-grass'?'Rumput liar':t.kind==='conifer'?'Pohon cemara laut':'Pohon jeruk';const image=t.kind==='bush'?(t.variant==='flower'?'assets/bush-jasmine.png':t.variant==='berry'?'assets/bush-mulberry.png':'assets/bush-selected.png'):t.kind==='wild-grass'?'assets/wild-grass-selected.png':t.kind==='conifer'?'assets/cemara-laut-redesign.png':'assets/orange-tree-pixel.png';row(`${label} · ${columnLabel(t.column)}${t.row+1}`,image,'trees',i,t);});
    (world.bridges||[]).forEach((b,i)=>row(`Jembatan ${b.material==='wood'?'kayu':'batu'} · ${columnLabel(b.column)}${b.row+1} · ${b.length||3}×${b.centered?2:b.width||1}`,`assets/bridge-${b.material}${b.width?'-wide':'-reference'}.png`,'bridges',i,b));
    (world.fences||[]).forEach((f,i)=>row(`${f.material==='stone-tall'?'Tembok tinggi':f.material==='wood'?'Pagar kayu':'Pagar batu'} · ${columnLabel(f.column)}${f.row+1} · ${f.edge}`,`assets/fence-mesh-${f.material}.png`,'fences',i,f));
    if(!libraryList.childElementCount){const empty=document.createElement('p');empty.textContent='Belum ada objek yang ditampilkan.';libraryList.append(empty);}
  }
  $('catalog-bulk-delete').addEventListener('click',()=>{
    const world=window.worldManager.getActive(),selected=catalogSelection.slice(),before=objectSets(world);
    if(!selected.length || !window.confirm(`Hapus ${selected.length} objek? Data panen terkait tidak dipindahkan.`))return;
    if(world.id!==worldId || selected.some(s=>JSON.stringify(before[s.key][s.index])!==JSON.stringify(s.object))){tell('EDITING · Objek berubah; data tidak diubah.');return;}
    const next={trees:before.trees.slice(),fences:before.fences.slice(),bridges:before.bridges.slice()};for(const s of selected.slice().sort((a,b)=>b.index-a.index))next[s.key].splice(s.index,1);
    if(saveCatalogObjects(before,next)){catalogSelection=[];catalogReplacement=null;renderLibrary();tell('EDITING · Objek terpilih dihapus.');}else tell('EDITING · Objek tidak dapat dihapus; data tidak diubah.');
  });
  $('catalog-bulk-replace').addEventListener('click',()=>{if(!catalogSelection.length)return;deactivateTerrain();syncControls();catalogReplacement={worldId:worldId,items:catalogSelection.map(s=>({...s})),expected:objectSets(window.worldManager.getActive()),label:`${catalogSelection.length} objek`};renderLibrary();selectCatalogTab(0);libraryDialog.scrollTop=0;});
  $('catalog-replace-cancel').addEventListener('click',()=>{catalogReplacement=null;renderLibrary();});
  libraryDialog.addEventListener('close',()=>{catalogReplacement=null;catalogSelection=[];});
  function bridgeDeck(tile) {const w=window.worldManager.getActive(),i=tile.row*w.gridSize+tile.column;return Math.max(1,w.blockStacks?.[i]?.length ?? (w.tiles?.[i]?1:0));}
  function movedBridge(b,tile) {return b.width!==undefined ? {...b,...tile} : {...b,...tile,gap:true,free:true,deck:bridgeDeck(tile)};}
  function replacementForButton(id,source) {
    const position={column:source.object.column,row:source.object.row};
    if(id==='catalog-place-orange')return {key:'trees',object:position};
    if(id==='catalog-place-conifer' || id==='catalog-place-conifer-small')return {key:'trees',object:{...position,kind:'conifer',...(id.endsWith('-small')?{size:1}:{})}};
    if(id==='catalog-place-wild-grass')return {key:'trees',object:{...position,kind:'wild-grass',size:1}};
    const bush=id.match(/^catalog-place-bush(?:-(flower|berry))?(-large)?$/);
    if(bush){const variant=bush[1],object={...position,kind:'bush',size:bush[2]?2:1,...(variant?{variant,plantId:(globalThis.crypto.randomUUID?.() || Array.from(globalThis.crypto.getRandomValues(new Uint8Array(16)),x=>x.toString(16).padStart(2,'0')).join('')),plantedAt:Date.now()}:{})};return {key:'trees',object};}
    const fence=id.match(/^catalog-place-fence-(wood|stone)$/);
    if(fence)return {key:'fences',object:{...position,edge:source.key==='fences'?source.object.edge:'S',material:fence[1]}};
    const bridge=id.match(/^catalog-place-bridge-(wood|stone)(?:-(4|5))?$/);
    if(bridge)return {key:'bridges',object:{...position,axis:source.key==='bridges'?source.object.axis:'E',material:bridge[1],gap:true,free:true,deck:source.key==='bridges' && source.object.free ? source.object.deck : bridgeDeck(position),straight:true,...(bridge[2]?{length:Number(bridge[2])}:{})}};
    return null;
  }
  libraryDialog.addEventListener('click',event=>{
    if(!catalogReplacement)return;const button=event.target.closest('.catalog-tile');if(!button)return;
    event.preventDefault();event.stopImmediatePropagation();const source=catalogReplacement,items=source.items||[source],replacements=items.map(item=>replacementForButton(button.id,item)),nextItem=replacements[0];
    if(!nextItem){$('catalog-replace-status').textContent='Karakter dan rumah player tidak dapat digunakan sebagai pengganti di sini.';return;}
    if(!window.confirm(`Ganti ${source.label}? Posisi awal dipertahankan; identitas dan siklus tanaman baru dimulai ulang.`))return;
    const before=source.expected,next={trees:before.trees.map(o=>({...o})),fences:before.fences.map(o=>({...o})),bridges:before.bridges.map(o=>({...o}))};
    if(source.worldId!==worldId || items.some(item=>JSON.stringify(before[item.key][item.index])!==JSON.stringify(item.object))){catalogReplacement=null;renderLibrary();tell('EDITING · Objek berubah; data tidak diubah.');return;}
    for(const item of items.slice().sort((a,b)=>b.index-a.index))next[item.key].splice(item.index,1);for(const item of replacements)next[item.key].push(item.object);
    if(saveCatalogObjects(before,next)){catalogReplacement=null;catalogSelection=[];renderLibrary();tell('EDITING · Objek diganti.');}else $('catalog-replace-status').textContent='Penggantian gagal: tapak, player, world, atau penyimpanan tidak aman. Objek lama tetap ada.';
  },true);
  $('object-library-button').addEventListener('click', () => {
    if(editing){clearGroup();selected=false;refresh();syncControls();} renderLibrary(); if (!libraryDialog.open) libraryDialog.showModal();
  });
  $('object-library-close').addEventListener('click', () => libraryDialog.close());
  window.addEventListener('worldchange', () => { if (libraryDialog.open) renderLibrary(); });
  const columns = $('object-column'), rows = $('object-row');
  for (let i = 0; i < 100; i++) {
    const col = document.createElement('option'); col.value = i; col.textContent = columnLabel(i); columns.append(col);
    const row = document.createElement('option'); row.value = i; row.textContent = String(i + 1); rows.append(row);
  }
  let editing = false, selected = false, selectedType = 'character', worldId = null, draft = null;
  let selectedBridgeIndex=null,placingBridge=null,bridgeAxis='E';
  let selectedTreeIndex=null, selectedFenceIndex=null, placingFence=null, placingFenceEdge='S', selectingObject=false, movingObject=false, objectPointer=null;
  let dragOffset = null, pinch = null, suppressDrag = false, objectDrag=null;
  let group=null, areaGesture=null, selectedBlock=null;
  const areaBox=document.createElement('div');areaBox.className='object-selection-area';areaBox.hidden=true;canvas.parentElement.append(areaBox);
  const pointers = new Map();
  function clearGroup(){group=null;areaGesture=null;areaBox.hidden=true;scene()?.setObjectGroup?.([],[]);}
  window.addEventListener('editorobjectsvisibilitychange', event=>{
    if(!editing || !event.detail?.hidden)return;
    clearGroup();selected=false;selectedTreeIndex=null;selectedFenceIndex=null;selectedBridgeIndex=null;selectedBlock=null;
    selectingObject=false;movingObject=false;placingHouse=false;placingOrange=false;placingFence=null;placingBridge=null;placementWorld=null;placementPointer=null;
    cancelGesture();tell('EDITING · Objek disembunyikan; data tetap tersimpan.');
  });
  // Starting placement or replacement reveals objects so they cannot be edited invisibly.
  libraryDialog.addEventListener('click',event=>{
    if(event.target.closest('[id^="catalog-place-"]') && scene()?.editorObjectsHidden?.())scene().setEditorObjectsHidden(false);
  },true);
  function groupSnapshot(){const w=window.worldManager.getActive();return {trees:(w.orangeTrees||[]).map(t=>({...t})),fences:(w.fences||[]).map(f=>({...f}))};}
  function saveGroup(before,next){applyingHistory=true;let ok;try{ok=window.worldManager.replaceObjectGroup(worldId,before,next);}finally{applyingHistory=false;}if(ok){history.undo.push({type:'group',value:before});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();}return ok;}
  function showGroup(){const w=window.worldManager.getActive();scene()?.setObjectGroup?.(group?.trees.map(i=>w.orangeTrees[i])||[],group?.fences.map(i=>w.fences[i])||[]);}

  // History is local to a world and this browser tab; reopening Edit keeps it until reload.
  const histories = new Map();
  const HISTORY_LIMIT = 50;
  let placingHouse=false, placingOrange=false, placementWorld=null, placementPointer=null;
  $('catalog-place-house').addEventListener('click', () => {
    if($('catalog-place-house').disabled || !editing) return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {
      const button=$(id); if(button?.getAttribute('aria-pressed')==='true') button.click();
    }
    selected=false; selectingObject=false; movingObject=false; draft=null; refresh(); syncControls();
    placingOrange=false; placingFence=null; placingHouse=true; placementWorld=window.worldManager.getActive().id; libraryDialog.close();syncControls();
    tell('EDITING · Ketuk tile untuk sudut pertama rumah player 3×3. Escape untuk batal.');
  });
  $('catalog-place-orange').addEventListener('click',()=>{
    if($('catalog-place-orange').disabled || !editing) return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingBridge=null;placingOrange=true;placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk sudut pertama pohon jeruk 2×2. Escape untuk batal.');
  });
  $('catalog-place-conifer').addEventListener('click',()=>{
    if($('catalog-place-conifer').disabled || !editing)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingBridge=null;placingOrange='conifer';placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk sudut pertama pohon cemara laut 2×2. Escape untuk batal.');
  });
  $('catalog-place-conifer-small').addEventListener('click',()=>{
    if($('catalog-place-conifer-small').disabled || !editing)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingBridge=null;placingOrange='conifer-small';placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk tile pohon cemara laut 1×1. Escape untuk batal.');
  });
  $('catalog-place-wild-grass').addEventListener('click',()=>{
    if($('catalog-place-wild-grass').disabled || !editing)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingBridge=null;placingOrange='wild-grass';placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk tile rumput liar 1×1. Escape untuk batal.');
  });
  for(const [suffix,variant,size] of [['',null,1],['-large',null,2],['-flower','flower',1],['-flower-large','flower',2],['-berry','berry',1],['-berry-large','berry',2]]) {
    $('catalog-place-bush'+suffix).addEventListener('click',()=>{
      if($('catalog-place-bush'+suffix).disabled || !editing)return;
      for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
      selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingBridge=null;placingOrange={kind:'bush',size,...(variant?{variant}:{})};placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
      tell(`EDITING · Ketuk tile sudut pertama semak ${size}×${size}. Escape untuk batal.`);
    });
  }
  for(const material of ['wood','stone']) $('catalog-place-fence-'+material).addEventListener('click',()=>{
    if(!editing || $('catalog-place-fence-'+material).disabled)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;placingHouse=false;placingFence=null;placingBridge=null;placingOrange=false;placingFence=material;placingFenceEdge='S';placementWorld=window.worldManager.getActive().id;refresh();syncControls();libraryDialog.close();
    tell('EDITING · Ketuk tile untuk pagar di sisi selatan, lalu pilih Putar untuk mengubah sisi.');
  });
  for(const material of ['wood','stone']) for(const length of [3,4,5]) {const button=$('catalog-place-bridge-'+material+(length===3?'':'-'+length));button.addEventListener('click',()=>{
    if(!editing || button.disabled)return;
    deactivateTerrain();clearGroup();selected=false;selectingObject=false;movingObject=false;draft=null;placingBridge={material,length};bridgeAxis='E';placementWorld=window.worldManager.getActive().id;libraryDialog.close();refresh();syncControls();
    tell(`EDITING · Ketuk awal jembatan ${length} block. Putar untuk arah; Batal untuk berhenti.`);
  });}
  canvas.addEventListener('pointerdown', event => {
    if((!placingHouse && !placingOrange && !placingFence && !placingBridge) || !editing || event.button!==0) return;
    event.preventDefault(); event.stopImmediatePropagation(); placementPointer=event.pointerId;
    const tile=scene()?.tileAt(scene().toLocal(event.clientX,event.clientY));
    if(placingBridge) {
      const before=window.worldManager.getActive().bridges||[],next=tile && [...before,{...tile,axis:bridgeAxis,material:placingBridge.material,gap:true,free:true,deck:bridgeDeck(tile),straight:true,...(placingBridge.length===3?{}:{length:placingBridge.length})}];
      if(!next || !saveBridges(before,next)){tell(`EDITING · Tapak jembatan harus di dalam world dan tidak bertumpuk dengan objek lain.`);return;}
      selectedType='bridge';selectedBridgeIndex=before.length;selected=true;refresh();syncControls();tell('EDITING · Jembatan ditempatkan. Ketuk tile lain atau Batal.');return;
    }
    if(placingFence) {
      const before=window.worldManager.getActive().fences||[];
      if(!tile || !saveFences(before,[...before,{...tile,edge:placingFenceEdge,material:placingFence}])) {tell('EDITING · Pilih tile kering dengan sisi yang belum berpagar.');return;}
      selectedType='fence';selectedFenceIndex=before.length;selected=true;refresh();syncControls();tell('EDITING · Pagar ditempatkan. Ketuk tile lain atau Batal penempatan.');return;
    }
    if(placingOrange) {
      const before=window.worldManager.getActive().orangeTrees||[];
      applyingHistory=true;let saved=false;
      try {if(tile)saved=window.worldManager.replaceOrangeTrees(placementWorld,before,[...before,typeof placingOrange==='object'?{...tile,...placingOrange,...(placingOrange.variant?{plantId:(globalThis.crypto.randomUUID?.() || Array.from(globalThis.crypto.getRandomValues(new Uint8Array(16)),x=>x.toString(16).padStart(2,'0')).join('')),plantedAt:Date.now()}:{})}:placingOrange==='wild-grass'?{...tile,kind:'wild-grass',size:1}:placingOrange==='conifer-small'?{...tile,kind:'conifer',size:1}:placingOrange==='conifer'?{...tile,kind:'conifer'}:tile]);} finally {applyingHistory=false;}
      if(!saved) {tell(`EDITING · Pilih area ${typeof placingOrange==='object'?`${placingOrange.size}×${placingOrange.size}`:placingOrange==='conifer-small' || placingOrange==='wild-grass'?'1×1':'2×2'} rata tanpa air, lubang, rumah, pohon, atau player.`);return;}
      const treeLabel=typeof placingOrange==='object'?'Semak':placingOrange==='wild-grass'?'Rumput liar':placingOrange==='conifer' || placingOrange==='conifer-small'?'Pohon cemara laut':'Pohon jeruk';
      history.undo.push({type:'orangeTrees',value:before.map(t=>({...t}))});
      if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();syncControls();tell(`EDITING · ${treeLabel} ditempatkan. Ketuk tile lain atau Batal penempatan.`);return;
    }
    const house=tile && {column:tile.column,row:tile.row,scale:1,facing:'S',visible:true};
    applyingHistory=true;
    let saved=false;
    try { if(house) saved=window.worldManager.replacePlayerHouse(placementWorld,null,house); }
    finally { applyingHistory=false; }
    if(!saved) { tell('EDITING · Tidak dapat ditempatkan. Pilih area 3×3 rata, tanpa air, lubang, atau player.'); return; }
    placingHouse=false; placementWorld=null;
    history.undo.push({type:'housePlacement',value:null});
    if(history.undo.length>HISTORY_LIMIT)history.undo.shift();
    history.redo.length=0;history.last=snapshot();selectedType='house';selected=true;draft=null;refresh();syncControls();
    tell('EDITING · Rumah player ditempatkan.');
  },true);
  for(const type of ['pointerup','pointercancel']) canvas.addEventListener(type,event=>{
    if(placementPointer!==null && event.pointerId===placementPointer) {event.preventDefault();event.stopImmediatePropagation();placementPointer=null;}
  },true);
  let history = null, applyingHistory = false, pendingTerrain = null;
  function same(a, b) {
    if (!a || !b) return a === b;
    return a.visible === b.visible && a.column === b.column && a.row === b.row
      && a.scale === b.scale && a.facing === b.facing && (a.opacity ?? 0) === (b.opacity ?? 0);
  }
  function current(type = selectedType) {
    const obj = type === 'block' ? (selectedBlock ? {...selectedBlock,opacity:(window.worldManager.getActive().blockOpacity||[]).find(e=>e.column===selectedBlock.column && e.row===selectedBlock.row && e.level===selectedBlock.level)?.opacity ?? 0} : null) : type === 'house' ? window.worldManager.getHouse() : type==='bridge' ? window.worldManager.getActive().bridges?.[selectedBridgeIndex] : type==='tree' ? window.worldManager.getActive().orangeTrees?.[selectedTreeIndex] : type==='fence' ? window.worldManager.getActive().fences?.[selectedFenceIndex] : window.worldManager.getCharacter();
    return obj ? {...obj} : null;
  }
  function blocks(world) { return world.blockStacks ? world.blockStacks.map(x=>x.slice()) : world.tiles?.map(t=>t?[t]:[]) ?? null; }
  function snapshot() { return {character: current('character'), house: current('house'), trees:(window.worldManager.getActive().orangeTrees||[]).map(t=>({...t})), fences:(window.worldManager.getActive().fences||[]).map(f=>({...f})), bridges:(window.worldManager.getActive().bridges||[]).map(b=>({...b})), blocks: blocks(window.worldManager.getActive()),blockOpacity:(window.worldManager.getActive().blockOpacity||[]).map(e=>({...e})),ramps:(window.worldManager.getActive().ramps||[]).map(r=>({...r}))}; }
  function sameBlocks(a,b) { return a===b || (Array.isArray(a) && Array.isArray(b) && a.length===b.length && a.every((v,i)=>v.length===b[i].length && v.every((t,j)=>t===b[i][j]))); }
  function sameSnapshot(a, b) { return a && b && same(a.character, b.character) && same(a.house, b.house) && JSON.stringify(a.trees)===JSON.stringify(b.trees) && JSON.stringify(a.fences)===JSON.stringify(b.fences) && JSON.stringify(a.bridges)===JSON.stringify(b.bridges) && sameBlocks(a.blocks,b.blocks) && JSON.stringify(a.blockOpacity)===JSON.stringify(b.blockOpacity) && JSON.stringify(a.ramps)===JSON.stringify(b.ramps); }
  function ensureHistory(id) {
    const state = snapshot();
    let item = histories.get(id);
    if (!item || !sameSnapshot(item.last, state)) {
      item = {undo: [], redo: [], last: state}; histories.set(id, item);
    }
    return item;
  }
  function saveWithoutRecording(config, type = selectedType) {
    applyingHistory = true;
    try { return type === 'house' ? window.worldManager.updateHouse(worldId, config)
      : window.worldManager.updateCharacter(worldId, config); }
    finally { applyingHistory = false; }
  }
  function recordEdit(config) {
    const before = current();
    if (!before) return false;
    if (same(before, config)) return true;
    if (!saveWithoutRecording(config)) return false;
    history.undo.push({type: selectedType, value: before});
    if (history.undo.length > HISTORY_LIMIT) history.undo.shift();
    history.redo.length = 0; history.last = snapshot(); syncControls(); return true;
  }
  function stepHistory(from, to, label) {
    if (!editing || !history || pendingTerrain || pointers.size || dialog.open || !from.length) return;
    const next = from[from.length - 1];
    if(next.type==='catalogObjects'){const before=objectSets(window.worldManager.getActive());applyingHistory=true;let saved;try{saved=window.worldManager.replaceCatalogObjects(worldId,before,next.value);}finally{applyingHistory=false;}if(!saved){tell('EDITING · Riwayat objek tidak dapat diterapkan.');return;}from.pop();to.push({type:'catalogObjects',value:before});history.last=snapshot();selected=false;refresh();syncControls();tell(`EDITING · ${label}: objek.`);return;}
    if(next.type==='group') {const before=groupSnapshot();applyingHistory=true;let saved;try{saved=window.worldManager.replaceObjectGroup(worldId,before,next.value);}finally{applyingHistory=false;}if(!saved){tell('EDITING · Riwayat grup tidak dapat diterapkan.');return;}from.pop();to.push({type:'group',value:before});history.last=snapshot();clearGroup();selected=false;movingObject=false;refresh();syncControls();tell(`EDITING · ${label}: grup objek.`);return;}
    if(next.type==='bridges') {const before=(window.worldManager.getActive().bridges||[]).map(b=>({...b}));applyingHistory=true;let saved;try{saved=window.worldManager.replaceBridges(worldId,before,next.value);}finally{applyingHistory=false;}if(!saved){tell('EDITING · Riwayat jembatan tidak dapat diterapkan.');return;}from.pop();to.push({type:'bridges',value:before});history.last=snapshot();selected=false;selectedBridgeIndex=null;refresh();syncControls();return;}
    if(next.type==='fences') {
      const before=(window.worldManager.getActive().fences||[]).map(f=>({...f}));
      applyingHistory=true;let saved;try{saved=window.worldManager.replaceFences(worldId,before,next.value);}finally{applyingHistory=false;}
      if(!saved){tell('EDITING · Riwayat pagar tidak dapat diterapkan.');return;}
      from.pop();to.push({type:'fences',value:before});history.last=snapshot();selected=false;selectedFenceIndex=null;movingObject=false;refresh();syncControls();return;
    }
    if(next.type==='orangeTrees') {
      const before=(window.worldManager.getActive().orangeTrees||[]).map(t=>({...t}));
      applyingHistory=true;let saved;try {saved=window.worldManager.replaceOrangeTrees(worldId,before,next.value);}finally{applyingHistory=false;}
      if(!saved){tell('EDITING · Riwayat pohon tidak dapat diterapkan; periksa tapak.');return;}
      from.pop();to.push({type:'orangeTrees',value:before});history.last=snapshot();draft=null;selected=false;selectedTreeIndex=null;movingObject=false;refresh();syncControls();tell(`EDITING · ${label}: objek.`);return;
    }
    if (next.type === 'housePlacement') {
      const before=current('house');
      applyingHistory=true; let saved;
      try { saved=window.worldManager.replacePlayerHouse(worldId,before,next.value); } finally { applyingHistory=false; }
      if(!saved) {tell('EDITING · Riwayat rumah tidak dapat diterapkan; periksa tapak dan player.');return;}
      from.pop();to.push({type:'housePlacement',value:before});
      history.last=snapshot();selectedType='house';selected=Boolean(next.value?.visible);draft=null;refresh();syncControls();
      tell(`EDITING · ${label}: penempatan rumah player.`);return;
    }
    if(next.type==='ramps') {
      const before=(window.worldManager.getActive().ramps||[]).map(r=>({...r}));applyingHistory=true;let saved;
      try{saved=window.worldManager.replaceRamps(worldId,before,next.value);}finally{applyingHistory=false;}
      if(!saved){tell('EDITING · Riwayat ramp tidak dapat diterapkan; periksa tapak.');return;}
      from.pop();to.push({type:'ramps',value:before});history.last=snapshot();refresh();syncControls();return;
    }
    if(next.type==='blockOpacity') {
      const before=(window.worldManager.getActive().blockOpacity||[]).map(e=>({...e}));applyingHistory=true;let saved;
      try{saved=window.worldManager.replaceBlockOpacity(worldId,before,next.value);}finally{applyingHistory=false;}
      if(!saved){tell('EDITING · Riwayat transparansi block tidak dapat disimpan.');return;}
      from.pop();to.push({type:'blockOpacity',value:before});history.last=snapshot();refresh();syncControls();return;
    }
    if (next.type === 'tiles') {
      const before=blocks(window.worldManager.getActive()),beforeOpacity=(window.worldManager.getActive().blockOpacity||[]).map(e=>({...e}));
      if (!before) return;
      applyingHistory=true;
      let saved;
      try { saved=window.worldManager.updateBlockStacks(worldId,next.value,next.opacity); } finally { applyingHistory=false; }
      if (!saved) { tell('EDITING · Riwayat block tidak dapat disimpan.'); return; }
      from.pop();to.push({type:'tiles',value:before,opacity:beforeOpacity});
      if(to.length>HISTORY_LIMIT)to.shift();
      history.last=snapshot();draft=null;refresh();syncControls();tell(`EDITING · ${label}: perubahan block.`);return;
    }
    const before = current(next.type);
    if (!before || !saveWithoutRecording(next.value, next.type)) { tell('EDITING · Riwayat tidak dapat disimpan.'); return; }
    from.pop(); to.push({type: next.type, value: before});
    if (to.length > HISTORY_LIMIT) to.shift();
    selectedType = next.type; history.last = snapshot(); draft = null; selected = next.value.visible;
    refresh(); syncControls();
    tell(`EDITING · ${label}: ${selectedType === 'house' ? 'Rumah' : 'Karakter'} ${columnLabel(next.value.column)}${next.value.row + 1}, skala ${Math.round(next.value.scale * 100)}%.`);
  }
  // A drag is one undo step, even though cells are safely saved as the pointer moves.
  window.editorBlockHistory=Object.freeze({
    begin() {
      if (!editing && document.querySelector('.preview-stage')?.dataset.mode==='editor') editButton.click();
      const world=window.worldManager.getActive();
      if (!editing || !history || world.id!==worldId || !Array.isArray(world.tiles) || pointers.size || dialog.open) return false;
      pendingTerrain={id:worldId,before:blocks(world),opacity:(world.blockOpacity||[]).map(e=>({...e})),changed:false};syncControls();return true;
    },
    update(id,stacks) {
      if (!pendingTerrain || pendingTerrain.id!==id || id!==worldId || !history) return false;
      applyingHistory=true;let saved;
      try { saved=window.worldManager.updateBlockStacks(id,stacks); } finally { applyingHistory=false; }
      if(saved) { pendingTerrain.changed=true;history.last=snapshot(); }
      return saved;
    },
    end() {
      const pending=pendingTerrain;pendingTerrain=null;
      if (pending?.changed && editing && history && pending.id===worldId && window.worldManager.getActive().id===worldId && (!sameBlocks(pending.before,blocks(window.worldManager.getActive())) || JSON.stringify(pending.opacity)!==JSON.stringify(window.worldManager.getActive().blockOpacity||[]))) {
        history.undo.push({type:'tiles',value:pending.before,opacity:pending.opacity});
        if(history.undo.length>HISTORY_LIMIT)history.undo.shift();
        history.redo.length=0;history.last=snapshot();
      }
      syncControls();
    }
  });
  window.editorRampHistory=Object.freeze({
    update(id,next) {
      if(!editing && document.querySelector('.preview-stage')?.dataset.mode==='editor')editButton.click();
      if(!editing || !history || id!==worldId || pendingTerrain || pointers.size || dialog.open)return false;
      const before=(window.worldManager.getActive().ramps||[]).map(r=>({...r}));
      if(JSON.stringify(before)===JSON.stringify(next))return true;
      applyingHistory=true;let saved;try{saved=window.worldManager.replaceRamps(id,before,next);}finally{applyingHistory=false;}
      if(saved){history.undo.push({type:'ramps',value:before});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();syncControls();}
      return saved;
    }
  });
  const scene = () => window.sceneEditing;
  const tell = text => { status.textContent = text; };
  function syncScale() { $('object-scale-value').textContent = `${$('object-scale').value}%`; }
  function syncControls() {
    editButton.classList.toggle('is-editing', editing);
    editButton.setAttribute('aria-pressed', String(editing));
    canvas.classList.toggle('is-editing', editing);
    status.hidden = !editing;
    const opacityControl=$('house-transparency-control'),opacity=$('house-transparency');
    opacityControl.hidden=!(editing && selected && ['house','tree','fence','bridge','block'].includes(selectedType));
    if(!opacityControl.hidden){$('transparency-label').textContent=`Transparansi ${selectedType==='house'?'rumah':selectedType==='tree'?'tanaman':selectedType==='fence'?'pagar':selectedType==='block'?'block':'jembatan'}`;opacity.value=String(current()?.opacity ?? 0);$('house-transparency-value').textContent=`${opacity.value}%`;}
    detailsButton.hidden = !editing || !selected || (selectedType==='tree'||selectedType==='fence'||selectedType==='bridge'||selectedType==='block');
    selectButton.hidden = !editing || (selected && selectedType === 'character') || !window.worldManager.getCharacter().visible;
    selectHouse.hidden = !editing || (selected && selectedType === 'house') || !window.worldManager.getHouse();
    undoButton.disabled = !editing || !history || !history.undo.length || pointers.size > 0 || Boolean(pendingTerrain) || Boolean(objectDrag?.started);
    redoButton.disabled = !editing || !history || !history.redo.length || pointers.size > 0 || Boolean(pendingTerrain) || Boolean(objectDrag?.started);
    rotateControls.hidden = !editing;
    rotateLeft.disabled = Boolean(group) || selectedType==='block' && selected || (!selected && !placingFence && !placingBridge) || pointers.size > 0;
    rotateRight.disabled = Boolean(group) || selectedType==='block' && selected || (!selected && !placingFence && !placingBridge) || pointers.size > 0;
    $('object-placement-cancel').hidden=!editing || !(placingHouse || placingOrange || placingFence || placingBridge);
    $('object-tool-select').setAttribute('aria-pressed',String(selectingObject));
    $('object-tool-move').disabled=!editing || (selected && selectedType==='block') || (!selected && !group);
    $('object-tool-move').setAttribute('aria-pressed',String(movingObject));
    $('object-tool-delete').disabled=!editing || (!group && (!selected || selectedType==='character' || selectedType==='block'));
    $('rotate-direction').textContent = directionNames[placingFence ? placingFenceEdge : (draft || current() || {}).facing] || 'Selatan';
  }
  function finishEditing() {
    if(pendingTerrain) window.editorBlockHistory.end();
    clearGroup();selectingObject=false;movingObject=false;selectedTreeIndex=null;selectedFenceIndex=null;selectedBridgeIndex=null;placingBridge=null;placingFence=null;placingHouse=false; placingOrange=false; placementWorld=null; pendingTerrain = null; editing = false; selected = false; worldId = null; draft = null; history = null;
    pointers.clear(); dragOffset = null; pinch = null; suppressDrag = false;objectPointer=null;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);
    scene()?.update(null, false); scene()?.setSelectedTree?.(null);scene()?.setSelectedFence?.(null);scene()?.setSelectedBridge?.(null); syncControls();
  }
  editButton.addEventListener('click', () => {
    if (editing) { finishEditing(); return; }
    const world = window.worldManager.getActive();
    if (world.id === 'base') {
      status.hidden = false; tell('Base World terkunci. Buka world buatan untuk mengedit.'); return;
    }
    editing = true; worldId = world.id; selected = false; draft = null;
    history = ensureHistory(worldId);
    syncControls(); tell('EDITING · Ketuk objek untuk memilih.');
  });
  undoButton.addEventListener('click', () => stepHistory(history?.undo, history?.redo, 'Undo'));
  redoButton.addEventListener('click', () => stepHistory(history?.redo, history?.undo, 'Redo'));
  function rotate(step) {
    if (!editing || pointers.size || dialog.open) return;
    if(placingBridge && (!selected || selectedType!=='bridge')){bridgeAxis=bridgeAxis==='E'?'S':'E';tell('EDITING · Arah jembatan diubah.');return;}
    if(selectedType==='bridge' && selected){const before=window.worldManager.getActive().bridges||[],b=before[selectedBridgeIndex];if(!b)return;const next=before.map((item,i)=>i===selectedBridgeIndex?{...(item.width!==undefined?item:movedBridge(item,item)),axis:item.axis==='E'?'S':'E'}:item);if(saveBridges(before,next)){if(placingBridge)bridgeAxis=next[selectedBridgeIndex].axis;refresh();syncControls();tell('EDITING · Jembatan diputar.');}else tell('EDITING · Arah itu tidak memiliki tapak yang aman.');return;}
    if(placingFence && (!selected || selectedType!=='fence')) {placingFenceEdge=directions[(directions.indexOf(placingFenceEdge)+step+4)%4];syncControls();tell(`EDITING · Sisi pagar ${directionNames[placingFenceEdge]}.`);return;}
    if(!selected)return;
    if(selectedType==='fence') {
      const before=window.worldManager.getActive().fences||[],f=before[selectedFenceIndex];if(!f)return;
      const next=before.map((item,i)=>i===selectedFenceIndex?{...item,edge:directions[(directions.indexOf(f.edge)+step+4)%4]}:item);
      if(saveFences(before,next)){if(placingFence)placingFenceEdge=next[selectedFenceIndex].edge;refresh();syncControls();tell('EDITING · Sisi pagar diperbarui.');}else tell('EDITING · Sisi ini sudah berpagar.');return;
    }
    if(selectedType==='tree') {
      const trees=window.worldManager.getActive().orangeTrees||[], tree=trees[selectedTreeIndex];if(!tree)return;
      const next=trees.map((t,i)=>i===selectedTreeIndex?{...t,facing:directions[(directions.indexOf(t.facing||'S')+step+4)%4]}:{...t});
      if(saveTrees(trees,next)) {refresh();syncControls();tell('EDITING · Arah pohon diperbarui.');}
      return;
    }
    const config = current();
    config.facing = directions[(directions.indexOf(config.facing) + step + 4) % 4];
    if (recordEdit(config)) {
      draft = null; refresh(); syncControls();
      tell(`EDITING · Menghadap ${directionNames[config.facing]}.`);
    } else tell('EDITING · Perubahan arah tidak tersimpan.');
  }
  rotateLeft.addEventListener('click', () => rotate(-1));
  rotateRight.addEventListener('click', () => rotate(1));
  selectButton.addEventListener('click', () => {
    if (!editing || !window.worldManager.getCharacter().visible) return;
    deactivateTerrain();selectedType = 'character'; selected = true; draft = null; refresh(); syncControls();
    tell('EDITING · Karakter dipilih. Geser atau gunakan Pengaturan objek.');
    detailsButton.focus();
  });
  selectHouse.addEventListener('click', () => {
    if (!editing || !window.worldManager.getHouse()) return;
    deactivateTerrain();selectedType = 'house'; selected = true; draft = null; refresh(); syncControls();
    tell('EDITING · Rumah dipilih. Gunakan Pengaturan objek atau geser rumah.');
    detailsButton.focus();
  });
  detailsButton.addEventListener('click', () => {
    if (!editing || !selected || window.worldManager.getActive().id !== worldId) return;
    const world = window.worldManager.getActive();
    const character = current();
    if (!character) return;
    $('object-world-name').textContent = `World: ${world.name}`;
    $('object-kind-title').textContent = selectedType === 'house' ? 'Rumah' : 'Karakter utama';
    $('object-visible-label').textContent = selectedType === 'house' ? 'Tampilkan rumah' : 'Tampilkan karakter';
    $('object-move-label').textContent = selectedType === 'house' ? 'Move · sudut A1 pada tapak 3×3' : 'Move · pusat tile';
    $('object-help').textContent = selectedType === 'house' ? `Kolom dan baris adalah sudut pertama tapak 3×3; pilih A–${columnLabel((world.gridSize || 9) - 3)} dan 1–${(world.gridSize || 9) - 2}. Ukuran gambar 150% dapat melewati tapak.` : 'Kaki karakter terkunci pada pusat tile. Pengaturan ini juga dapat digunakan tanpa gestur sentuh.';
    for (const opt of columns.options) opt.disabled = Number(opt.value) >= (window.worldManager.getActive().gridSize || 9) - (selectedType === 'house' ? 2 : 0);
    for (const opt of rows.options) opt.disabled = Number(opt.value) >= (window.worldManager.getActive().gridSize || 9) - (selectedType === 'house' ? 2 : 0);
    $('object-visible').checked = character.visible;
    columns.value = character.column; rows.value = character.row;
    $('object-scale').value = Math.round(character.scale * 100); syncScale();
    $('object-message').textContent = '';
    if (!dialog.open) dialog.showModal();
  });
  $('house-transparency').addEventListener('input',event=>{$('house-transparency-value').textContent=`${event.target.value}%`;});
  $('house-transparency').addEventListener('change',event=>{
    if(!editing || !selected || !['house','tree','fence','bridge','block'].includes(selectedType))return;
    const config=current(),value=Number(event.target.value);if(!config)return;
    if(value===0)delete config.opacity;else config.opacity=value;
    let saved=false;
    if(selectedType==='house')saved=recordEdit(config);
    else if(selectedType==='block') {
      const before=(window.worldManager.getActive().blockOpacity||[]).map(e=>({...e}));
      const next=before.filter(e=>!(e.column===config.column && e.row===config.row && e.level===config.level));
      if(value>0)next.push({column:config.column,row:config.row,level:config.level,opacity:value});
      applyingHistory=true;try{saved=window.worldManager.replaceBlockOpacity(worldId,before,next);}finally{applyingHistory=false;}
      if(saved && JSON.stringify(before)!==JSON.stringify(next)){history.undo.push({type:'blockOpacity',value:before});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();}
    }
    else {
      const world=window.worldManager.getActive(),key=selectedType==='tree'?'orangeTrees':selectedType==='fence'?'fences':'bridges';
      const index=selectedType==='tree'?selectedTreeIndex:selectedType==='fence'?selectedFenceIndex:selectedBridgeIndex;
      const before=world[key]||[],next=before.map((item,i)=>i===index?config:item);
      if(index!==null && before[index] && JSON.stringify(before[index])===JSON.stringify(config))saved=true;
      else if(index!==null && before[index])saved=selectedType==='tree'?saveTrees(before,next):selectedType==='fence'?saveFences(before,next):saveBridges(before,next);
    }
    if(saved){refresh();syncControls();tell('EDITING · Transparansi objek disimpan bersama world.');}
    else {syncControls();tell('EDITING · Transparansi tidak dapat disimpan.');}
  });
  $('object-scale').addEventListener('input', syncScale);
  $('object-close').addEventListener('click', () => dialog.close());
  $('object-cancel').addEventListener('click', () => dialog.close());
  form.addEventListener('submit', event => {
    event.preventDefault();
    const config = {...current(), visible: $('object-visible').checked, column: Number(columns.value),
      row: Number(rows.value), scale: Number($('object-scale').value) / 100,
      facing: current().facing};
    if (recordEdit(config)) {
      dialog.close(); selected = config.visible; draft = null;
      refresh(); syncControls();
      tell(selected ? 'EDITING · Objek dipilih. Geser atau cubit untuk mengubah ukuran.' : selectedType === 'house' ? 'EDITING · Rumah disembunyikan. Gunakan Pilih rumah untuk menampilkannya kembali.' : 'EDITING · Karakter disembunyikan. Aktifkan kembali lewat World → Edit.');
    } else $('object-message').textContent = 'Perubahan tidak disimpan. Pastikan world masih aktif dan penyimpanan browser tersedia.';
  });
  function position(event) { return {x: event.clientX, y: event.clientY}; }
  function distance(a, b) { return Math.hypot(a.x - b.x, a.y - b.y); }
  function refresh() { scene()?.setSelectedBlock?.(editing && selected && selectedType==='block' ? selectedBlock : null);scene()?.update(draft, editing && selected && selectedType!=='tree' && selectedType!=='fence' && selectedType!=='bridge' && selectedType!=='block', selectedType);scene()?.setSelectedTree?.(editing && selected && selectedType==='tree' ? current('tree') : null);scene()?.setSelectedFence?.(editing && selected && selectedType==='fence' ? current('fence') : null);scene()?.setSelectedBridge?.(editing && selected && selectedType==='bridge' ? current('bridge') : null); }
  function cancelGesture() { pointers.clear(); pinch = null; dragOffset = null; suppressDrag = false; draft = null;objectPointer=null;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);refresh(); syncControls(); }
  function deactivateTerrain() {
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    placingHouse=false;placingFence=null;placingBridge=null;placingOrange=false;placementWorld=null;
  }
  $('object-placement-cancel').addEventListener('click',()=>{placingHouse=false;placingOrange=false;placingFence=null;placingBridge=null;placementWorld=null;syncControls();tell('EDITING · Penempatan objek dibatalkan.');});
  function saveCatalogObjects(before,next) {if(!editing || !history)return false;applyingHistory=true;let saved;try{saved=window.worldManager.replaceCatalogObjects(worldId,before,next);}finally{applyingHistory=false;}if(saved){history.undo.push({type:'catalogObjects',value:before});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();syncControls();}return saved;}
  function saveBridges(before,next) {applyingHistory=true;let saved;try{saved=window.worldManager.replaceBridges(worldId,before,next);}finally{applyingHistory=false;}if(saved){history.undo.push({type:'bridges',value:before.map(b=>({...b}))});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();}return saved;}
  function saveFences(before,next) {
    applyingHistory=true;let saved;try{saved=window.worldManager.replaceFences(worldId,before,next);}finally{applyingHistory=false;}
    if(saved){history.undo.push({type:'fences',value:before.map(f=>({...f}))});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();}return saved;
  }
  function saveTrees(before,next) {
    applyingHistory=true;let saved;
    try {saved=window.worldManager.replaceOrangeTrees(worldId,before,next);}finally{applyingHistory=false;}
    if(saved){history.undo.push({type:'orangeTrees',value:before.map(t=>({...t}))});if(history.undo.length>HISTORY_LIMIT)history.undo.shift();history.redo.length=0;history.last=snapshot();}
    return saved;
  }
  $('object-tool-select').addEventListener('click',()=>{
    if(!editing)return;deactivateTerrain();clearGroup();selected=false;selectingObject=!selectingObject;movingObject=false;refresh();syncControls();tell(selectingObject?'EDITING · Seret area untuk memilih pagar dan tanaman; ketuk untuk memilih rumah, objek, atau block.':'EDITING · Pilih objek dinonaktifkan.');
  });
  $('object-tool-move').addEventListener('click',()=>{
    if(!editing || (selected && selectedType==='block') || (!selected && !group))return;deactivateTerrain();selectingObject=false;movingObject=!movingObject;syncControls();
    tell(movingObject?'EDITING · Ketuk tile tujuan untuk objek. Escape untuk batal.':'EDITING · Pindahkan dibatalkan.');
  });
  $('object-tool-delete').addEventListener('click',()=>{
    if(!editing || (!group && (!selected || selectedType==='character' || selectedType==='block')) || !window.confirm('Hapus objek yang dipilih dari world?'))return;
    if(group){const before=groupSnapshot(),next={trees:before.trees.filter((_,i)=>!group.trees.includes(i)),fences:before.fences.filter((_,i)=>!group.fences.includes(i))};if(saveGroup(before,next)){clearGroup();selected=false;movingObject=false;refresh();syncControls();tell('EDITING · Grup dihapus. Undo untuk memulihkan.');}else tell('EDITING · Grup tidak dapat dihapus.');return;}
    let saved=false;
    if(selectedType==='bridge') {const before=window.worldManager.getActive().bridges||[];saved=saveBridges(before,before.filter((_,i)=>i!==selectedBridgeIndex));}
    else if(selectedType==='fence') {const before=window.worldManager.getActive().fences||[];saved=saveFences(before,before.filter((_,i)=>i!==selectedFenceIndex));}
    else if(selectedType==='tree') {
      const before=window.worldManager.getActive().orangeTrees||[];saved=saveTrees(before,before.filter((_,i)=>i!==selectedTreeIndex));
    } else if(selectedType==='house') {
      const before=current('house');applyingHistory=true;try{saved=window.worldManager.replacePlayerHouse(worldId,before,null);}finally{applyingHistory=false;}
      if(saved){history.undo.push({type:'housePlacement',value:before});history.redo.length=0;history.last=snapshot();}
    }
    if(saved){deactivateTerrain();selected=false;selectedTreeIndex=null;movingObject=false;draft=null;refresh();syncControls();tell('EDITING · Objek dihapus. Undo untuk memulihkan.');}
    else tell('EDITING · Objek tidak dapat dihapus.');
  });
  function moveGroupBy(dc,dr){
    const before=groupSnapshot(),next={trees:before.trees.map((o,i)=>group.trees.includes(i)?{...o,column:o.column+dc,row:o.row+dr}:o),fences:before.fences.map((o,i)=>group.fences.includes(i)?{...o,column:o.column+dc,row:o.row+dr}:o)};
    if(dc===0 && dr===0)return true;
    if(!saveGroup(before,next))return false;group.anchor={column:group.anchor.column+dc,row:group.anchor.row+dr};showGroup();return true;
  }
  // Selection area is measured in screen coordinates; object anchors decide inclusion.
  canvas.addEventListener('pointerdown',event=>{
    if(!editing || event.button!==0)return;
    if(areaGesture){areaGesture=null;areaBox.hidden=true;event.preventDefault();event.stopImmediatePropagation();return;}
    const local=scene().toLocal(event.clientX,event.clientY);
    if(group && !movingObject && !selectingObject && !placingHouse && !placingOrange && !placingFence && !document.querySelector('.terrain-tools button[aria-pressed="true"]') && $('editor-pan').getAttribute('aria-pressed')!=='true') {
      const fi=scene().fenceAt(local),ti=scene().orangeTreeAt(local);
      if(group.fences.includes(fi) || group.trees.includes(ti)) {areaGesture={id:event.pointerId,x:event.clientX,y:event.clientY,local,drag:true,start:scene().tileAt(local)};event.preventDefault();event.stopImmediatePropagation();try{canvas.setPointerCapture(event.pointerId);}catch(_){}return;}
      clearGroup();
    }
    if(!selectingObject && !(group && movingObject))return;
    if(group && movingObject){
      event.preventDefault();event.stopImmediatePropagation();objectPointer=event.pointerId;
      const tile=scene().tileAt(local);if(!tile){tell('EDITING · Pilih tile tujuan dalam world.');return;}
      const dc=tile.column-group.anchor.column,dr=tile.row-group.anchor.row;
      if(moveGroupBy(dc,dr)){movingObject=false;tell('EDITING · Grup dipindahkan. Undo untuk membatalkan.');}else tell('EDITING · Tujuan tidak valid; grup tidak berubah.');syncControls();return;
    }
    areaGesture={id:event.pointerId,x:event.clientX,y:event.clientY,local};
    event.preventDefault();event.stopImmediatePropagation();try{canvas.setPointerCapture(event.pointerId);}catch(_){}
  },true);
  canvas.addEventListener('pointermove',event=>{
    if(!areaGesture || areaGesture.id!==event.pointerId)return;
    event.preventDefault();event.stopImmediatePropagation();if(areaGesture.drag)return;const r=canvas.getBoundingClientRect(),x=Math.min(areaGesture.x,event.clientX),y=Math.min(areaGesture.y,event.clientY);
    areaBox.hidden=false;Object.assign(areaBox.style,{left:(x-r.left)+'px',top:(y-r.top)+'px',width:Math.abs(event.clientX-areaGesture.x)+'px',height:Math.abs(event.clientY-areaGesture.y)+'px'});
  },true);
  for(const type of ['pointerup','pointercancel'])canvas.addEventListener(type,event=>{
    if(!areaGesture || areaGesture.id!==event.pointerId)return;
    event.preventDefault();event.stopImmediatePropagation();const g=areaGesture;areaGesture=null;areaBox.hidden=true;
    if(type==='pointercancel'){tell('EDITING · Seleksi area dibatalkan.');return;}
    const end=scene().toLocal(event.clientX,event.clientY),w=window.worldManager.getActive();
    if(g.drag){const dest=scene().tileAt(end);if(Math.hypot(event.clientX-g.x,event.clientY-g.y)>=5){if(g.start && dest && moveGroupBy(dest.column-g.start.column,dest.row-g.start.row))tell('EDITING · Grup dipindahkan. Undo untuk membatalkan.');else tell('EDITING · Tujuan tidak valid; grup tidak berubah.');}syncControls();return;}
    if(Math.hypot(event.clientX-g.x,event.clientY-g.y)<5){
      const bi=scene().bridgeAt(end),fi=scene().fenceAt(end),ti=scene().orangeTreeAt(end),house=window.worldManager.getHouse(),player=window.worldManager.getCharacter();clearGroup();
      if(scene().hit(end,player)){selected=true;selectedType='character';}
      else if(bi!==null){selected=true;selectedType='bridge';selectedBridgeIndex=bi;}
      else if(fi!==null){selected=true;selectedType='fence';selectedFenceIndex=fi;}
      else if(ti!==null){selected=true;selectedType='tree';selectedTreeIndex=ti;}
      else if(house?.visible && scene().houseAt(end,house)){selected=true;selectedType='house';}
      else {selectedBlock=scene().blockAt?.(end) || null;selected=!!selectedBlock;if(selected)selectedType='block';}
      selectingObject=false;draft=null;refresh();syncControls();tell(selected?(selectedType==='block'?`EDITING · Block ${columnLabel(selectedBlock.column)}${selectedBlock.row+1}, tingkat ${selectedBlock.level+1} dipilih.`:'EDITING · Objek dipilih.'):'EDITING · Tidak ada objek atau block di titik ini.');return;
    }
    const inside=p=>p.x>=Math.min(g.local.x,end.x) && p.x<=Math.max(g.local.x,end.x) && p.y>=Math.min(g.local.y,end.y) && p.y<=Math.max(g.local.y,end.y);
    const trees=[],fences=[];(scene().editorObjectsHidden?.()?[]:(w.orangeTrees||[])).forEach((t,i)=>{if(inside(scene().orangeTreeAnchor(t)))trees.push(i);});(scene().editorObjectsHidden?.()?[]:(w.fences||[])).forEach((f,i)=>{const {a,b}=scene().fenceGeometry(f);if(inside({x:(a.x+b.x)/2,y:(a.y+b.y)/2}))fences.push(i);});
    const first=trees.length?w.orangeTrees[trees[0]]:fences.length?w.fences[fences[0]]:null;
    group=first?{trees,fences,anchor:{column:first.column,row:first.row}}:null;selected=false;selectingObject=false;showGroup();refresh();syncControls();tell(group?`EDITING · ${trees.length+fences.length} objek dipilih. Gunakan Geser atau Hapus.`:'EDITING · Tidak ada titik dasar objek di area.');
  },true);
  // Capture object tools before terrain, pan, and the older drag interaction.
  canvas.addEventListener('pointerdown',event=>{
    if(objectDrag && event.pointerId!==objectDrag.id) { objectPointer=null;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);refresh();syncControls();event.preventDefault();event.stopImmediatePropagation();return; }
    if(!editing || event.button!==0 || placingHouse || placingOrange || placingFence || placingBridge)return;
    const local=scene()?.toLocal(event.clientX,event.clientY);if(!local)return;
    const terrainActive=!!document.querySelector('.terrain-tools button[aria-pressed="true"]');
    const directBridge=!terrainActive && $('editor-pan').getAttribute('aria-pressed')!=='true' ? (scene()?.bridgeAt?.(local) ?? null) : null;
    const directTree=!terrainActive && $('editor-pan').getAttribute('aria-pressed')!=='true' ? (scene()?.orangeTreeAt?.(local) ?? null) : null;
    const directFence=!terrainActive && $('editor-pan').getAttribute('aria-pressed')!=='true' ? (scene()?.fenceAt?.(local) ?? null) : null;
    if(!selectingObject && !movingObject && directTree===null && directFence===null && directBridge===null)return;
    event.preventDefault();event.stopImmediatePropagation();objectPointer=event.pointerId;
    if(movingObject) {
      const tile=scene().tileAt(local);if(!tile){tell('EDITING · Pilih tile di dalam world.');return;}
      let saved=false;
      if(selectedType==='bridge') {const before=window.worldManager.getActive().bridges||[];saved=saveBridges(before,before.map((b,i)=>i===selectedBridgeIndex?movedBridge(b,tile):b));}
      else if(selectedType==='fence') {const before=window.worldManager.getActive().fences||[];saved=saveFences(before,before.map((f,i)=>i===selectedFenceIndex?{...f,...tile}:f));}
      else if(selectedType==='tree') {
        const before=window.worldManager.getActive().orangeTrees||[];
        const next=before.map((t,i)=>i===selectedTreeIndex?{...t,...tile}:{...t});saved=saveTrees(before,next);
      } else if(selectedType==='house') {
        const before=current('house');applyingHistory=true;try{saved=window.worldManager.moveHouse(worldId,before,tile.column,tile.row);}finally{applyingHistory=false;}
        if(saved){history.undo.push({type:'house',value:before});history.redo.length=0;history.last=snapshot();}
      } else {
        const before=current('character'),house=window.worldManager.getHouse();
        if(!(house?.visible && tile.column>=house.column && tile.column<house.column+3 && tile.row>=house.row && tile.row<house.row+3))saved=recordEdit({...before,...tile});
      }
      if(saved){movingObject=false;refresh();syncControls();tell('EDITING · Objek dipindahkan.');}
      else tell('EDITING · Tujuan tidak valid atau terhalang; objek tidak dipindahkan.');
      return;
    }
    const player=window.worldManager.getCharacter(), house=window.worldManager.getHouse();
    if(scene().hit(local,player)) {selectedType='character';selectedTreeIndex=null;selected=true;}
    else if(directBridge!==null) {selectedType='bridge';selectedBridgeIndex=directBridge;selected=true;}
    else if(directFence!==null && directFence!==undefined) {selectedType='fence';selectedFenceIndex=directFence;selected=true;}
    else if(directTree!==null && directTree!==undefined) {selectedType='tree';selectedTreeIndex=directTree;selected=true;}
    else if(house && scene().houseAt(local,house)) {selectedType='house';selectedTreeIndex=null;selected=true;}
    else {selected=false;selectedTreeIndex=null;}
    draft=null;
    if(selected && (selectedType==='tree'||selectedType==='fence'||selectedType==='bridge')) {
      const source=current(selectedType),index=selectedType==='tree'?selectedTreeIndex:selectedType==='bridge'?selectedBridgeIndex:selectedFenceIndex;
      objectDrag={id:event.pointerId,kind:selectedType,index,source,start:local,startX:event.clientX,startY:event.clientY,anchor:scene().centre(source),target:null,started:false};
      try {canvas.setPointerCapture(event.pointerId);}catch(_){}
    }
    selectingObject=false;refresh();syncControls();tell(selected?'EDITING · Objek dipilih; seret untuk memindahkan.':'EDITING · Tidak ada objek di titik ini.');
  },true);
  canvas.addEventListener('pointermove',event=>{
    if(!objectDrag || objectDrag.id!==event.pointerId)return;
    event.preventDefault();event.stopImmediatePropagation();
    if(!objectDrag.started && Math.hypot(event.clientX-objectDrag.startX,event.clientY-objectDrag.startY)<5)return;
    objectDrag.started=true;
    const local=scene().toLocal(event.clientX,event.clientY),point={x:objectDrag.anchor.x+local.x-objectDrag.start.x,y:objectDrag.anchor.y+local.y-objectDrag.start.y};
    const tile=scene().tileAt(point);objectDrag.target=tile;
    if(tile){const ghost={...objectDrag.source,...tile};scene().setObjectDrag(objectDrag.kind,objectDrag.index,ghost);
      if(objectDrag.kind==='tree')scene().setSelectedTree(ghost);else if(objectDrag.kind==='bridge')scene().setSelectedBridge(ghost);else scene().setSelectedFence(ghost);
    }else scene().setObjectDrag(null,0,null);
    syncControls();
  },true);
  for(const type of ['pointerup','pointercancel'])canvas.addEventListener(type,event=>{
    if(objectPointer===null || objectPointer!==event.pointerId)return;
    event.preventDefault();event.stopImmediatePropagation();objectPointer=null;
    const drag=objectDrag;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);
    if(type==='pointerup' && drag?.started) {
      const tile=drag.target;
      if(!tile){tell('EDITING · Tujuan di luar world; objek tidak dipindahkan.');}
      else if(tile.column===drag.source.column && tile.row===drag.source.row){tell('EDITING · Objek tetap di posisi semula.');}
      else {
        const key=drag.kind==='tree'?'orangeTrees':drag.kind==='bridge'?'bridges':'fences',before=window.worldManager.getActive()[key]||[];
        const same=JSON.stringify(before[drag.index])===JSON.stringify(drag.source);
        const next=before.map((o,i)=>i===drag.index?(drag.kind==='bridge'?movedBridge(o,tile):{...o,...tile}):o);
        const saved=same && (drag.kind==='tree'?saveTrees(before,next):drag.kind==='bridge'?saveBridges(before,next):saveFences(before,next));
        tell(saved?'EDITING · Objek dipindahkan. Undo untuk membatalkan.':'EDITING · Tujuan terhalang; objek tidak dipindahkan.');
      }
    } else if(type==='pointercancel' && drag?.started)tell('EDITING · Geser dibatalkan.');
    refresh();syncControls();
  },true);
  for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) $(id)?.addEventListener('click',()=>{
    clearGroup();selectingObject=false;movingObject=false;placingHouse=false;placingOrange=false;placingFence=null;placingBridge=null;placementWorld=null;
    if(selectedType==='block'){selected=false;selectedBlock=null;refresh();}syncControls();
  });
  canvas.addEventListener('pointerdown', event => {
    if (!editing || !scene()) return;
    event.preventDefault();
    if (pointers.size === 0) {
      const character = window.worldManager.getCharacter(), house = window.worldManager.getHouse();
      const local = scene().toLocal(event.clientX, event.clientY);
      if (scene().hit(local, character)) selectedType = 'character';
      else if (house && scene().hitHouse?.(local, house)) selectedType = 'house';
      else {
        selected = false; draft = null; refresh(); syncControls();
        tell('EDITING · Ketuk objek untuk memilih.'); return;
      }
      selected = true; draft = {...current()};
      const anchor = selectedType === 'house' ? scene().centreHouse(draft) : scene().centre(draft);
      dragOffset = {x: local.x - anchor.x, y: local.y - anchor.y};
      suppressDrag = false;
      tell(`EDITING · ${selectedType === 'house' ? 'Rumah' : 'Karakter'} dipilih. Geser atau cubit untuk mengubah ukuran.`);
      syncControls(); refresh();
    }
    if (!selected || pointers.size >= 2) return;
    pointers.set(event.pointerId, position(event));
    syncControls();
    try { canvas.setPointerCapture(event.pointerId); } catch (_) { /* optional in test environments */ }
    if (pointers.size === 2) {
      const [a, b] = [...pointers.values()];
      pinch = {distance: Math.max(1, distance(a, b)), scale: draft.scale};
      dragOffset = null; suppressDrag = true;
    }
  });
  canvas.addEventListener('pointermove', event => {
    if (!editing || !pointers.has(event.pointerId) || !draft) return;
    event.preventDefault(); pointers.set(event.pointerId, position(event));
    if (pointers.size === 2 && pinch) {
      const [a, b] = [...pointers.values()];
      draft = {...draft, scale: Math.round(Math.max(0.5, Math.min(1.5,
        pinch.scale * distance(a, b) / pinch.distance)) * 100) / 100};
      refresh();
    } else if (pointers.size === 1 && dragOffset && !suppressDrag) {
      const local = scene().toLocal(event.clientX, event.clientY);
      const tile = scene().tile({x: local.x - dragOffset.x, y: local.y - dragOffset.y});
      if (selectedType === 'house') {
        tile.column = Math.max(0, Math.min((window.worldManager.getActive().gridSize || 9) - 3, tile.column - 1));
        tile.row = Math.max(0, Math.min((window.worldManager.getActive().gridSize || 9) - 3, tile.row - 1));
      }
      draft = {...draft, ...tile}; refresh();
    }
  });
  canvas.addEventListener('pointerup', event => {
    if (!editing || !pointers.has(event.pointerId)) return;
    event.preventDefault(); pointers.delete(event.pointerId);
    if (pointers.size > 0) { pinch = null; suppressDrag = true; return; }
    const pending = draft; dragOffset = null; pinch = null; suppressDrag = false;
    if (pending && recordEdit(pending)) {
      draft = null; refresh(); syncControls();
      tell(`EDITING · ${selectedType === 'house' ? 'Rumah' : 'Karakter'} di ${columnLabel(pending.column)}${pending.row + 1}, skala ${Math.round(pending.scale * 100)}%.`);
    } else { draft = null; refresh(); syncControls(); tell('EDITING · Perubahan tidak tersimpan. Periksa penyimpanan browser.'); }
  });
  canvas.addEventListener('pointercancel', event => {
    if (editing && pointers.has(event.pointerId)) { cancelGesture(); tell('EDITING · Gerakan dibatalkan.'); }
  });
  window.addEventListener('worldchange', () => {
    if (editing && window.worldManager.getActive().id !== worldId) {
      if (dialog.open) dialog.close(); finishEditing();
    } else if (editing && !applyingHistory && history && !sameSnapshot(history.last, snapshot())) {
      // Other tabs or world settings may change this character; don't undo over newer data.
      pendingTerrain = null; clearGroup(); cancelGesture();
      history = ensureHistory(worldId);
      selected = (selectedType==='tree'||selectedType==='fence'||selectedType==='bridge'||selectedType==='block') ? false : Boolean(current()?.visible) && selected;
      if(!selected)selectedTreeIndex=null;movingObject=false;
      refresh(); syncControls();
      tell('EDITING · Objek berubah di luar editor; riwayat disetel ulang.');
    }
  });
  document.addEventListener('keydown', event => {
    if(event.key==='Escape' && group && !movingObject && !selectingObject){event.preventDefault();clearGroup();refresh();syncControls();tell('EDITING · Pilihan grup dibatalkan.');return;}
    if (event.key==='Escape' && (movingObject || selectingObject)) {event.preventDefault();movingObject=false;selectingObject=false;syncControls();tell('EDITING · Alat objek dibatalkan.');return;}
    if (event.key === 'Escape' && (placingHouse || placingOrange || placingFence || placingBridge)) { event.preventDefault(); placingHouse=false; placingOrange=false; placingFence=null; placingBridge=null; placementWorld=null; syncControls();tell('EDITING · Penempatan objek dibatalkan.'); return; }
    if (event.key === 'Escape' && editing && !dialog.open) { finishEditing(); return; }
    const tag = event.target?.tagName?.toLowerCase();
    if (!editing || dialog.open || !(event.ctrlKey || event.metaKey) || event.altKey
        || tag === 'input' || tag === 'textarea' || tag === 'select' || event.target?.isContentEditable) return;
    const key = event.key.toLowerCase();
    if (key === 'z' && event.shiftKey && !redoButton.disabled) { event.preventDefault(); redoButton.click(); }
    else if (key === 'z' && !event.shiftKey && !undoButton.disabled) { event.preventDefault(); undoButton.click(); }
    else if (key === 'y' && !redoButton.disabled) { event.preventDefault(); redoButton.click(); }
  });
})();
