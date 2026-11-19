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
    for(const kind of ['wood','stone']) $('catalog-place-fence-'+kind).disabled=world.id==='base' || !Array.isArray(world.tiles);
    $('catalog-place-wild-grass').disabled=world.id==='base' || !Array.isArray(world.tiles);
    $('catalog-house-status').textContent=world.id==='base' ? 'Base World terkunci. Buka world buatan untuk menempatkan rumah.' : exists ? 'World ini sudah memiliki rumah player. Pindahkan rumah yang ada bila diperlukan.' : !Array.isArray(world.tiles) ? 'World lama ini tidak mendukung penempatan rumah.' : 'Pilih tombol, lalu ketuk sudut pertama tapak pada peta.';
    const objects = [];
    const character = window.worldManager.getCharacter();
    if (character.visible) objects.push(`Karakter · ${columnLabel(character.column)}${character.row + 1}`);
    const house = window.worldManager.getHouse?.();
    if (house) objects.push(`Rumah player · ${columnLabel(house.column)}${house.row + 1}–${columnLabel(house.column + 2)}${house.row + 3}${house.visible ? '' : ' (tersembunyi)'}`);
    for(const tree of world.orangeTrees||[]) objects.push(`${tree.kind==='bush'?(tree.variant==='flower'?'Semak melati':tree.variant==='berry'?'Semak murbei':'Semak'):tree.kind==='wild-grass'?'Rumput liar':tree.kind==='conifer'?'Pohon cemara laut':'Pohon jeruk'} · ${columnLabel(tree.column)}${tree.row+1}–${columnLabel(tree.column+(tree.size||2)-1)}${tree.row+(tree.size||2)}`);
    for(const fence of world.fences||[]) objects.push(`Pagar ${fence.material==='wood'?'kayu':'batu'} · ${columnLabel(fence.column)}${fence.row+1} · ${fence.edge}`);
    if (!objects.length) objects.push('Belum ada objek yang ditampilkan.');
    for (const label of objects) {
      const row = document.createElement('div');
      row.className = 'world-card'; row.textContent = label; libraryList.append(row);
    }
  }
  $('object-library-button').addEventListener('click', () => {
    renderLibrary(); if (!libraryDialog.open) libraryDialog.showModal();
  });
  $('object-library-close').addEventListener('click', () => libraryDialog.close());
  window.addEventListener('worldchange', () => { if (libraryDialog.open) renderLibrary(); });
  const columns = $('object-column'), rows = $('object-row');
  for (let i = 0; i < 100; i++) {
    const col = document.createElement('option'); col.value = i; col.textContent = columnLabel(i); columns.append(col);
    const row = document.createElement('option'); row.value = i; row.textContent = String(i + 1); rows.append(row);
  }
  let editing = false, selected = false, selectedType = 'character', worldId = null, draft = null;
  let selectedTreeIndex=null, selectedFenceIndex=null, placingFence=null, placingFenceEdge='S', selectingObject=false, movingObject=false, objectPointer=null;
  let dragOffset = null, pinch = null, suppressDrag = false, objectDrag=null;
  const pointers = new Map();
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
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingOrange=true;placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk sudut pertama pohon jeruk 2×2. Escape untuk batal.');
  });
  $('catalog-place-conifer').addEventListener('click',()=>{
    if($('catalog-place-conifer').disabled || !editing)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingOrange='conifer';placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk sudut pertama pohon cemara laut 2×2. Escape untuk batal.');
  });
  $('catalog-place-conifer-small').addEventListener('click',()=>{
    if($('catalog-place-conifer-small').disabled || !editing)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingOrange='conifer-small';placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk tile pohon cemara laut 1×1. Escape untuk batal.');
  });
  $('catalog-place-wild-grass').addEventListener('click',()=>{
    if($('catalog-place-wild-grass').disabled || !editing)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingOrange='wild-grass';placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
    tell('EDITING · Ketuk tile rumput liar 1×1. Escape untuk batal.');
  });
  for(const [suffix,variant,size] of [['',null,1],['-large',null,2],['-flower','flower',1],['-flower-large','flower',2],['-berry','berry',1],['-berry-large','berry',2]]) {
    $('catalog-place-bush'+suffix).addEventListener('click',()=>{
      if($('catalog-place-bush'+suffix).disabled || !editing)return;
      for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
      selected=false;selectingObject=false;movingObject=false;draft=null;refresh();syncControls();placingHouse=false;placingFence=null;placingOrange={kind:'bush',size,...(variant?{variant}:{})};placementWorld=window.worldManager.getActive().id;libraryDialog.close();syncControls();
      tell(`EDITING · Ketuk tile sudut pertama semak ${size}×${size}. Escape untuk batal.`);
    });
  }
  for(const material of ['wood','stone']) $('catalog-place-fence-'+material).addEventListener('click',()=>{
    if(!editing || $('catalog-place-fence-'+material).disabled)return;
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    selected=false;selectingObject=false;movingObject=false;draft=null;placingHouse=false;placingFence=null;placingOrange=false;placingFence=material;placingFenceEdge='S';placementWorld=window.worldManager.getActive().id;refresh();syncControls();libraryDialog.close();
    tell('EDITING · Ketuk tile untuk pagar di sisi selatan, lalu pilih Putar untuk mengubah sisi.');
  });
  canvas.addEventListener('pointerdown', event => {
    if((!placingHouse && !placingOrange && !placingFence) || !editing || event.button!==0) return;
    event.preventDefault(); event.stopImmediatePropagation(); placementPointer=event.pointerId;
    const tile=scene()?.tileAt(scene().toLocal(event.clientX,event.clientY));
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
      && a.scale === b.scale && a.facing === b.facing;
  }
  function current(type = selectedType) {
    const obj = type === 'house' ? window.worldManager.getHouse() : type==='tree' ? window.worldManager.getActive().orangeTrees?.[selectedTreeIndex] : type==='fence' ? window.worldManager.getActive().fences?.[selectedFenceIndex] : window.worldManager.getCharacter();
    return obj ? {...obj} : null;
  }
  function blocks(world) { return world.blockStacks ? world.blockStacks.map(x=>x.slice()) : world.tiles?.map(t=>t?[t]:[]) ?? null; }
  function snapshot() { return {character: current('character'), house: current('house'), trees:(window.worldManager.getActive().orangeTrees||[]).map(t=>({...t})), fences:(window.worldManager.getActive().fences||[]).map(f=>({...f})), blocks: blocks(window.worldManager.getActive())}; }
  function sameBlocks(a,b) { return a===b || (Array.isArray(a) && Array.isArray(b) && a.length===b.length && a.every((v,i)=>v.length===b[i].length && v.every((t,j)=>t===b[i][j]))); }
  function sameSnapshot(a, b) { return a && b && same(a.character, b.character) && same(a.house, b.house) && JSON.stringify(a.trees)===JSON.stringify(b.trees) && JSON.stringify(a.fences)===JSON.stringify(b.fences) && sameBlocks(a.blocks,b.blocks); }
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
    if (next.type === 'tiles') {
      const before=blocks(window.worldManager.getActive());
      if (!before) return;
      applyingHistory=true;
      let saved;
      try { saved=window.worldManager.updateBlockStacks(worldId,next.value); } finally { applyingHistory=false; }
      if (!saved) { tell('EDITING · Riwayat block tidak dapat disimpan.'); return; }
      from.pop();to.push({type:'tiles',value:before});
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
      pendingTerrain={id:worldId,before:blocks(world),changed:false};syncControls();return true;
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
      if (pending?.changed && editing && history && pending.id===worldId && window.worldManager.getActive().id===worldId && !sameBlocks(pending.before,blocks(window.worldManager.getActive()))) {
        history.undo.push({type:'tiles',value:pending.before});
        if(history.undo.length>HISTORY_LIMIT)history.undo.shift();
        history.redo.length=0;history.last=snapshot();
      }
      syncControls();
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
    detailsButton.hidden = !editing || !selected || (selectedType==='tree'||selectedType==='fence');
    selectButton.hidden = !editing || (selected && selectedType === 'character') || !window.worldManager.getCharacter().visible;
    selectHouse.hidden = !editing || (selected && selectedType === 'house') || !window.worldManager.getHouse();
    undoButton.disabled = !editing || !history || !history.undo.length || pointers.size > 0 || Boolean(pendingTerrain) || Boolean(objectDrag?.started);
    redoButton.disabled = !editing || !history || !history.redo.length || pointers.size > 0 || Boolean(pendingTerrain) || Boolean(objectDrag?.started);
    rotateControls.hidden = !editing;
    rotateLeft.disabled = (!selected && !placingFence) || pointers.size > 0;
    rotateRight.disabled = (!selected && !placingFence) || pointers.size > 0;
    $('object-placement-cancel').hidden=!editing || !(placingHouse || placingOrange || placingFence);
    $('object-tool-select').setAttribute('aria-pressed',String(selectingObject));
    $('object-tool-move').disabled=!editing || !selected;
    $('object-tool-move').setAttribute('aria-pressed',String(movingObject));
    $('object-tool-delete').disabled=!editing || !selected || selectedType==='character';
    $('rotate-direction').textContent = directionNames[placingFence ? placingFenceEdge : (draft || current() || {}).facing] || 'Selatan';
  }
  function finishEditing() {
    if(pendingTerrain) window.editorBlockHistory.end();
    selectingObject=false;movingObject=false;selectedTreeIndex=null;selectedFenceIndex=null;placingFence=null;placingHouse=false; placingOrange=false; placementWorld=null; pendingTerrain = null; editing = false; selected = false; worldId = null; draft = null; history = null;
    pointers.clear(); dragOffset = null; pinch = null; suppressDrag = false;objectPointer=null;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);
    scene()?.update(null, false); scene()?.setSelectedTree?.(null);scene()?.setSelectedFence?.(null); syncControls();
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
  $('object-scale').addEventListener('input', syncScale);
  $('object-close').addEventListener('click', () => dialog.close());
  $('object-cancel').addEventListener('click', () => dialog.close());
  form.addEventListener('submit', event => {
    event.preventDefault();
    const config = {visible: $('object-visible').checked, column: Number(columns.value),
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
  function refresh() { scene()?.update(draft, editing && selected && selectedType!=='tree' && selectedType!=='fence', selectedType);scene()?.setSelectedTree?.(editing && selected && selectedType==='tree' ? current('tree') : null);scene()?.setSelectedFence?.(editing && selected && selectedType==='fence' ? current('fence') : null); }
  function cancelGesture() { pointers.clear(); pinch = null; dragOffset = null; suppressDrag = false; draft = null;objectPointer=null;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);refresh(); syncControls(); }
  function deactivateTerrain() {
    for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) {const button=$(id);if(button?.getAttribute('aria-pressed')==='true')button.click();}
    placingHouse=false;placingFence=null;placingOrange=false;placementWorld=null;
  }
  $('object-placement-cancel').addEventListener('click',()=>{placingHouse=false;placingOrange=false;placingFence=null;placementWorld=null;syncControls();tell('EDITING · Penempatan objek dibatalkan.');});
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
    if(!editing)return;deactivateTerrain();selectingObject=!selectingObject;movingObject=false;syncControls();tell(selectingObject?'EDITING · Ketuk objek untuk memilih.':'EDITING · Pilih objek dinonaktifkan.');
  });
  $('object-tool-move').addEventListener('click',()=>{
    if(!editing || !selected)return;deactivateTerrain();selectingObject=false;movingObject=!movingObject;syncControls();
    tell(movingObject?'EDITING · Ketuk tile tujuan untuk objek. Escape untuk batal.':'EDITING · Pindahkan dibatalkan.');
  });
  $('object-tool-delete').addEventListener('click',()=>{
    if(!editing || !selected || selectedType==='character' || !window.confirm('Hapus objek yang dipilih dari world?'))return;
    let saved=false;
    if(selectedType==='fence') {const before=window.worldManager.getActive().fences||[];saved=saveFences(before,before.filter((_,i)=>i!==selectedFenceIndex));}
    else if(selectedType==='tree') {
      const before=window.worldManager.getActive().orangeTrees||[];saved=saveTrees(before,before.filter((_,i)=>i!==selectedTreeIndex));
    } else if(selectedType==='house') {
      const before=current('house');applyingHistory=true;try{saved=window.worldManager.replacePlayerHouse(worldId,before,null);}finally{applyingHistory=false;}
      if(saved){history.undo.push({type:'housePlacement',value:before});history.redo.length=0;history.last=snapshot();}
    }
    if(saved){deactivateTerrain();selected=false;selectedTreeIndex=null;movingObject=false;draft=null;refresh();syncControls();tell('EDITING · Objek dihapus. Undo untuk memulihkan.');}
    else tell('EDITING · Objek tidak dapat dihapus.');
  });
  // Capture object tools before terrain, pan, and the older drag interaction.
  canvas.addEventListener('pointerdown',event=>{
    if(objectDrag && event.pointerId!==objectDrag.id) { objectPointer=null;objectDrag=null;scene()?.setObjectDrag?.(null,0,null);refresh();syncControls();event.preventDefault();event.stopImmediatePropagation();return; }
    if(!editing || event.button!==0 || placingHouse || placingOrange || placingFence)return;
    const local=scene()?.toLocal(event.clientX,event.clientY);if(!local)return;
    const terrainActive=!!document.querySelector('.terrain-tools button[aria-pressed="true"]');
    const directTree=!terrainActive && $('editor-pan').getAttribute('aria-pressed')!=='true' ? (scene()?.orangeTreeAt?.(local) ?? null) : null;
    const directFence=!terrainActive && $('editor-pan').getAttribute('aria-pressed')!=='true' ? (scene()?.fenceAt?.(local) ?? null) : null;
    if(!selectingObject && !movingObject && directTree===null && directFence===null)return;
    event.preventDefault();event.stopImmediatePropagation();objectPointer=event.pointerId;
    if(movingObject) {
      const tile=scene().tileAt(local);if(!tile){tell('EDITING · Pilih tile di dalam world.');return;}
      let saved=false;
      if(selectedType==='fence') {const before=window.worldManager.getActive().fences||[];saved=saveFences(before,before.map((f,i)=>i===selectedFenceIndex?{...f,...tile}:f));}
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
    else if(directFence!==null && directFence!==undefined) {selectedType='fence';selectedFenceIndex=directFence;selected=true;}
    else if(directTree!==null && directTree!==undefined) {selectedType='tree';selectedTreeIndex=directTree;selected=true;}
    else if(house && scene().houseAt(local,house)) {selectedType='house';selectedTreeIndex=null;selected=true;}
    else {selected=false;selectedTreeIndex=null;}
    draft=null;
    if(selected && (selectedType==='tree'||selectedType==='fence')) {
      const source=current(selectedType),index=selectedType==='tree'?selectedTreeIndex:selectedFenceIndex;
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
      if(objectDrag.kind==='tree')scene().setSelectedTree(ghost);else scene().setSelectedFence(ghost);
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
        const key=drag.kind==='tree'?'orangeTrees':'fences',before=window.worldManager.getActive()[key]||[];
        const same=JSON.stringify(before[drag.index])===JSON.stringify(drag.source);
        const next=before.map((o,i)=>i===drag.index?{...o,...tile}:o);
        const saved=same && (drag.kind==='tree'?saveTrees(before,next):saveFences(before,next));
        tell(saved?'EDITING · Objek dipindahkan. Undo untuk membatalkan.':'EDITING · Tujuan terhalang; objek tidak dipindahkan.');
      }
    } else if(type==='pointercancel' && drag?.started)tell('EDITING · Geser dibatalkan.');
    refresh();syncControls();
  },true);
  for(const id of ['terrain-draw','terrain-add','terrain-erase','terrain-pick','terrain-fill','editor-pan']) $(id)?.addEventListener('click',()=>{
    selectingObject=false;movingObject=false;placingHouse=false;placingOrange=false;placingFence=null;placementWorld=null;syncControls();
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
      pendingTerrain = null; cancelGesture();
      history = ensureHistory(worldId);
      selected = (selectedType==='tree'||selectedType==='fence') ? false : Boolean(current()?.visible) && selected;
      if(!selected)selectedTreeIndex=null;movingObject=false;
      refresh(); syncControls();
      tell('EDITING · Objek berubah di luar editor; riwayat disetel ulang.');
    }
  });
  document.addEventListener('keydown', event => {
    if (event.key==='Escape' && (movingObject || selectingObject)) {event.preventDefault();movingObject=false;selectingObject=false;syncControls();tell('EDITING · Alat objek dibatalkan.');return;}
    if (event.key === 'Escape' && (placingHouse || placingOrange || placingFence)) { event.preventDefault(); placingHouse=false; placingOrange=false; placingFence=null; placementWorld=null; syncControls();tell('EDITING · Penempatan objek dibatalkan.'); return; }
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
