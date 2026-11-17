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
    const objects = [];
    const character = window.worldManager.getCharacter();
    if (character.visible) objects.push(`Karakter · ${columnLabel(character.column)}${character.row + 1}`);
    const house = window.worldManager.getHouse?.();
    if (house) objects.push(`Rumah · ${columnLabel(house.column)}${house.row + 1}–${columnLabel(house.column + 2)}${house.row + 3}${house.visible ? '' : ' (tersembunyi)'}`);
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
  let dragOffset = null, pinch = null, suppressDrag = false;
  const pointers = new Map();
  // History is local to a world and this browser tab; reopening Edit keeps it until reload.
  const histories = new Map();
  const HISTORY_LIMIT = 50;
  let history = null, applyingHistory = false, pendingTerrain = null;
  function same(a, b) {
    if (!a || !b) return a === b;
    return a.visible === b.visible && a.column === b.column && a.row === b.row
      && a.scale === b.scale && a.facing === b.facing;
  }
  function current(type = selectedType) {
    const obj = type === 'house' ? window.worldManager.getHouse() : window.worldManager.getCharacter();
    return obj ? {...obj} : null;
  }
  function snapshot() { const tiles=window.worldManager.getActive().tiles; return {character: current('character'), house: current('house'), tiles: Array.isArray(tiles) ? tiles.slice() : null}; }
  function sameTiles(a,b) { return a===b || (Array.isArray(a) && Array.isArray(b) && a.length===b.length && a.every((v,i)=>v===b[i])); }
  function sameSnapshot(a, b) { return a && b && same(a.character, b.character) && same(a.house, b.house) && sameTiles(a.tiles,b.tiles); }
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
    if (next.type === 'tiles') {
      const before=window.worldManager.getActive().tiles?.slice();
      if (!before) return;
      applyingHistory=true;
      let saved;
      try { saved=window.worldManager.updateTiles(worldId,next.value); } finally { applyingHistory=false; }
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
      pendingTerrain={id:worldId,before:world.tiles.slice(),changed:false};syncControls();return true;
    },
    update(id,tiles) {
      if (!pendingTerrain || pendingTerrain.id!==id || id!==worldId || !history) return false;
      applyingHistory=true;let saved;
      try { saved=window.worldManager.updateTiles(id,tiles); } finally { applyingHistory=false; }
      if(saved) { pendingTerrain.changed=true;history.last=snapshot(); }
      return saved;
    },
    end() {
      const pending=pendingTerrain;pendingTerrain=null;
      if (pending?.changed && editing && history && pending.id===worldId && window.worldManager.getActive().id===worldId && !sameTiles(pending.before,window.worldManager.getActive().tiles)) {
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
    detailsButton.hidden = !editing || !selected;
    selectButton.hidden = !editing || (selected && selectedType === 'character') || !window.worldManager.getCharacter().visible;
    selectHouse.hidden = !editing || (selected && selectedType === 'house') || !window.worldManager.getHouse();
    undoButton.disabled = !editing || !history || !history.undo.length || pointers.size > 0 || Boolean(pendingTerrain);
    redoButton.disabled = !editing || !history || !history.redo.length || pointers.size > 0 || Boolean(pendingTerrain);
    rotateControls.hidden = !editing || !selected;
    rotateLeft.disabled = pointers.size > 0;
    rotateRight.disabled = pointers.size > 0;
    $('rotate-direction').textContent = directionNames[(draft || current() || {}).facing] || 'Selatan';
  }
  function finishEditing() {
    if(pendingTerrain) window.editorBlockHistory.end();
    pendingTerrain = null; editing = false; selected = false; worldId = null; draft = null; history = null;
    pointers.clear(); dragOffset = null; pinch = null; suppressDrag = false;
    scene()?.update(null, false); syncControls();
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
    if (!editing || !selected || pointers.size || dialog.open) return;
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
    selectedType = 'character'; selected = true; draft = null; refresh(); syncControls();
    tell('EDITING · Karakter dipilih. Geser atau gunakan Pengaturan objek.');
    detailsButton.focus();
  });
  selectHouse.addEventListener('click', () => {
    if (!editing || !window.worldManager.getHouse()) return;
    selectedType = 'house'; selected = true; draft = null; refresh(); syncControls();
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
  function refresh() { scene()?.update(draft, editing && selected, selectedType); }
  function cancelGesture() { pointers.clear(); pinch = null; dragOffset = null; suppressDrag = false; draft = null; refresh(); syncControls(); }
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
      selected = Boolean(current()?.visible) && selected;
      refresh(); syncControls();
      tell('EDITING · Objek berubah di luar editor; riwayat disetel ulang.');
    }
  });
  document.addEventListener('keydown', event => {
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
