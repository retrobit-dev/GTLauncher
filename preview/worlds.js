(() => {
  'use strict';
  const KEY = 'gtlauncher.worlds.v1';
  const BASE = Object.freeze({id: 'base', name: 'Base World', description: 'Dasar bersama untuk semua world.', color: null});
  const BUILT_INS = [BASE];
  const dialog = document.getElementById('world-dialog');
  if (!dialog) return;
  const $ = id => document.getElementById(id);
  const list = $('world-list');
  const editor = $('world-editor');
  const form = $('world-form');
  const message = $('world-message');
  const activeLabel = $('active-world-label');
  let editingId = null;
  let pendingDelete = null;
  let state = {worlds: [], activeId: 'base'};
  let storageError = '';
  let exportUrl = null;
  let exportFile = null;
  function showExport(url, filename, qr, json = '') {
    if (exportUrl) URL.revokeObjectURL(exportUrl);
    exportUrl = url.startsWith('blob:') ? url : null;
    const image = $('world-export-qr'), link = $('world-export-download');
    image.hidden = !qr;
    if (qr) image.src = url; else image.removeAttribute('src');
    link.href = url; link.download = filename;
    link.textContent = qr ? 'Unduh gambar QR' : 'Unduh JSON';
    exportFile = qr ? null : new File([json], filename, {type: 'application/json'});
    $('world-export-save').hidden = !exportFile || typeof window.showSaveFilePicker !== 'function';
    let shareAllowed = false;
    try { shareAllowed = Boolean(exportFile && navigator.share && navigator.canShare?.({files: [exportFile]})); } catch (_) { /* not supported */ }
    $('world-export-share').hidden = !shareAllowed;
    $('world-export-file-status').textContent = '';
    $('world-export-text-section').hidden = qr;
    $('world-export-json').value = json;
    $('world-export-copy-status').textContent = '';
    $('world-export-help').textContent = qr
      ? 'Jika unduhan tidak muncul, tekan dan tahan gambar untuk menyimpannya, atau ambil tangkapan layar seluruh QR. Gambar ini berisi data world.'
      : 'Tekan Unduh JSON. Jika unduhan diblokir, salin isi JSON di bawah dan simpan sebagai file .json. JSON dipakai untuk impor Android.';
    $('world-export-result').hidden = false;
    $('world-export-result').scrollIntoView?.({block: 'nearest'});
  }
  window.addEventListener('pagehide', () => { if (exportUrl) URL.revokeObjectURL(exportUrl); });
  $('world-export-save').addEventListener('click', async () => {
    const file = exportFile;
    if (!file || typeof window.showSaveFilePicker !== 'function') return;
    const status = $('world-export-file-status');
    try {
      const handle = await window.showSaveFilePicker({suggestedName: file.name,
        types: [{description: 'World JSON', accept: {'application/json': ['.json']}}]});
      const stream = await handle.createWritable();
      await stream.write(file); await stream.close();
      status.textContent = 'File JSON berhasil disimpan.';
    } catch (error) {
      status.textContent = error?.name === 'AbortError' ? 'Penyimpanan dibatalkan.'
        : 'Browser tidak mengizinkan penyimpanan langsung. Coba Bagikan file JSON atau salin isi JSON di bawah.';
    }
  });
  $('world-export-share').addEventListener('click', async () => {
    const file = exportFile;
    if (!file || !navigator.share) return;
    const status = $('world-export-file-status');
    try {
      await navigator.share({files: [file], title: 'World GTLauncher'});
      status.textContent = 'Menu berbagi selesai. Periksa aplikasi atau lokasi yang dipilih.';
    } catch (error) {
      status.textContent = error?.name === 'AbortError' ? 'Berbagi dibatalkan.'
        : 'Browser tidak mengizinkan berbagi file. Gunakan tautan Unduh JSON atau salin isinya.';
    }
  });
  $('world-export-copy').addEventListener('click', async () => {
    const input = $('world-export-json');
    try {
      if (!navigator.clipboard?.writeText) throw new Error('clipboard unavailable');
      await navigator.clipboard.writeText(input.value);
      $('world-export-copy-status').textContent = 'JSON disalin.';
    } catch (_) {
      input.focus(); input.select();
      $('world-export-copy-status').textContent = 'Pilih dan salin teks secara manual; clipboard tidak tersedia di preview ini.';
    }
  });

  function validHouse(h) {
    return h === null || (h && typeof h === 'object' && !Array.isArray(h)
      && Object.keys(h).every(k => ['column', 'row', 'scale', 'facing', 'visible'].includes(k))
      && Number.isInteger(h.column) && h.column >= 0 && h.column <= 6
      && Number.isInteger(h.row) && h.row >= 0 && h.row <= 6
      && (h.scale === undefined || (Number.isFinite(h.scale) && h.scale >= 0.5 && h.scale <= 1.5))
      && (h.facing === undefined || ['N', 'E', 'S', 'W'].includes(h.facing))
      && (h.visible === undefined || typeof h.visible === 'boolean'));
  }
  function readState() {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw === null) return {worlds: [], activeId: 'base'};
      const value = JSON.parse(raw);
      if (!value || !Array.isArray(value.worlds) || typeof value.activeId !== 'string') throw new Error('format');
      const ids = new Set();
      const readableWorlds = [];
      let skipped = 0;
      for (const world of value.worlds) {
        if (!world || typeof world.id !== 'string' || !world.id || world.id === 'base' || ids.has(world.id)
          || typeof world.name !== 'string' || !world.name.trim() || world.name.length > 48
          || typeof world.description !== 'string' || world.description.length > 160
          || (world.house !== undefined && !validHouse(world.house))
          || (world.showCharacter !== undefined && typeof world.showCharacter !== 'boolean')
          || (world.characterFacing !== undefined && !['N', 'E', 'S', 'W'].includes(world.characterFacing))
          || (world.characterColumn !== undefined && (!Number.isInteger(world.characterColumn) || world.characterColumn < 0 || world.characterColumn > 8))
          || (world.characterRow !== undefined && (!Number.isInteger(world.characterRow) || world.characterRow < 0 || world.characterRow > 8))
          || (world.characterScale !== undefined && (!Number.isFinite(world.characterScale) || world.characterScale < 0.5 || world.characterScale > 1.5))
          || typeof world.color !== 'string' || !/^#[0-9a-f]{6}$/i.test(world.color)) { skipped++; continue; }
        ids.add(world.id);
        readableWorlds.push(world);
      }
      if (skipped) storageError = 'Sebagian data world tidak dapat dibaca. World yang valid ditampilkan dalam mode baca saja; data lama tidak akan ditimpa. Unduh cadangan sebelum melakukan pemulihan.';
      return {worlds: readableWorlds, activeId: BUILT_INS.some(world => world.id === value.activeId) || ids.has(value.activeId) ? value.activeId : 'base'};
    } catch (error) {
      storageError = 'Data world tidak dapat dibaca. Data lama tidak akan ditimpa; periksa penyimpanan browser.';
      return {worlds: [], activeId: 'base'};
    }
  }
  state = readState();
  // Add the requested house once to an existing Home, preserving every other field and world.
  // Failure leaves the original data untouched and is surfaced in the World dialog.
  if (!storageError && state.worlds.some(w => w.name.trim().toLocaleLowerCase() === 'home' && w.house === undefined)) {
    const next = {...state, worlds: state.worlds.map(w => w.name.trim().toLocaleLowerCase() === 'home' && w.house === undefined
      ? {...w, house: {column: 0, row: 0}} : w)};
    try { localStorage.setItem(KEY, JSON.stringify(next)); state = next; }
    catch (error) { storageError = 'Rumah belum dapat disimpan. Periksa izin atau ruang penyimpanan browser.'; }
  }

  function activeWorld() { return BUILT_INS.find(world => world.id === state.activeId)
    || state.worlds.find(world => world.id === state.activeId) || BASE; }
  function getCharacter(world = activeWorld()) {
    return {visible: world.id !== 'base' && (world.showCharacter ?? world.name.trim().toLocaleLowerCase() === 'home'),
      column: world.characterColumn ?? 4, row: world.characterRow ?? 4,
      scale: world.characterScale ?? 1, facing: world.characterFacing ?? 'S'};
  }
  function updateCharacter(id, config) {
    if (id === 'base' || activeWorld().id !== id || !state.worlds.some(world => world.id === id)
      || typeof config.visible !== 'boolean' || !Number.isInteger(config.column) || config.column < 0 || config.column > 8
      || !Number.isInteger(config.row) || config.row < 0 || config.row > 8
      || !Number.isFinite(config.scale) || config.scale < 0.5 || config.scale > 1.5
      || !['N', 'E', 'S', 'W'].includes(config.facing)) return false;
    const worlds = state.worlds.map(world => world.id === id ? {...world,
      showCharacter: config.visible, characterColumn: config.column,
      characterRow: config.row, characterScale: config.scale, characterFacing: config.facing} : world);
    return commit({...state, worlds});
  }
  function getHouse(world = activeWorld()) {
    if (world.id === 'base' || !world.house) return null;
    const house = world.house;
    return {column: house.column, row: house.row, scale: house.scale ?? 1,
      facing: house.facing ?? 'S', visible: house.visible ?? true};
  }
  function updateHouse(id, house) {
    if (id === 'base' || activeWorld().id !== id || !state.worlds.some(w => w.id === id && w.house)
      || !house || !validHouse(house) || typeof house.visible !== 'boolean'
      || typeof house.scale !== 'number' || !['N','E','S','W'].includes(house.facing)) return false;
    return commit({...state, worlds: state.worlds.map(w => w.id === id ? {...w, house: {...house}} : w)});
  }
  function displayName(world) { return world.name; }
  function announce(text) { message.textContent = text; }
  function commit(next) {
    if (storageError) { announce(storageError); return false; }
    try { localStorage.setItem(KEY, JSON.stringify(next)); }
    catch (error) { announce('World tidak dapat disimpan. Periksa izin atau ruang penyimpanan browser.'); return false; }
    state = next;
    render();
    window.dispatchEvent(new Event('worldchange'));
    return true;
  }
  // Parse the portable format without discarding unsupported fields or object types.
  function parseImport(text) {
    const fail = () => { throw new Error('Berkas tidak valid atau berisi data yang belum didukung.'); };
    const record = value => value !== null && typeof value === 'object' && !Array.isArray(value);
    const keys = (value, allowed) => record(value) && Object.keys(value).every(key => allowed.includes(key));
    let root;
    try { root = JSON.parse(text.replace(/^\uFEFF/, '')); } catch (_) { return fail(); }
    if (!keys(root, ['format', 'version', 'world']) || root.format !== 'gtlauncher-world'
      || ![1, 2, 3, 4, 5, 6].includes(root.version)) return fail();
    const input = root.world, version = root.version;
    if (!keys(input, version === 1 ? ['name', 'description', 'color'] : ['id', 'name', 'description', 'color', 'objects'])
      || typeof input.name !== 'string' || !input.name.trim() || input.name.length > 48
      || typeof input.description !== 'string' || input.description.length > 160
      || typeof input.color !== 'string' || !/^#[0-9a-f]{6}$/i.test(input.color)) return fail();
    const id = version === 1 ? (globalThis.crypto?.randomUUID?.() || `world-${Date.now()}-${Math.random().toString(36).slice(2)}`) : input.id;
    if (typeof id !== 'string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(id) || id === 'base') return fail();
    const objects = version === 1 ? [] : input.objects;
    if (!Array.isArray(objects) || objects.length > (version >= 5 ? 2 : version >= 3 ? 1 : 0)) return fail();
    let character = null, house = null;
    for (const obj of objects) {
      if (!record(obj)) return fail();
      if (obj.type === 'character' && version >= 3 && !character) {
        if (!keys(obj, version >= 4 ? ['type','column','row','scale','facing'] : ['type','column','row','scale'])
          || !Number.isInteger(obj.column) || obj.column < 0 || obj.column > 8
          || !Number.isInteger(obj.row) || obj.row < 0 || obj.row > 8
          || !Number.isFinite(obj.scale) || obj.scale < 0.5 || obj.scale > 1.5
          || (version >= 4 && !['N','E','S','W'].includes(obj.facing))) return fail();
        character = obj;
      } else if (obj.type === 'house' && version >= 5 && !house) {
        if (version === 5) {
          if (!keys(obj, ['type','column','row']) || obj.column !== 0 || obj.row !== 0) return fail();
          house = {column: 0, row: 0};
        } else {
          if (!keys(obj, ['type','column','row','scale','facing','visible'])
            || !Number.isInteger(obj.column) || obj.column < 0 || obj.column > 6
            || !Number.isInteger(obj.row) || obj.row < 0 || obj.row > 6
            || !Number.isFinite(obj.scale) || obj.scale < 0.5 || obj.scale > 1.5
            || !['N','E','S','W'].includes(obj.facing) || typeof obj.visible !== 'boolean') return fail();
          house = {column: obj.column, row: obj.row, scale: obj.scale, facing: obj.facing, visible: obj.visible};
        }
      } else return fail();
    }
    return {version, world: {id, name: input.name, description: input.description, color: input.color,
      showCharacter: Boolean(character), characterColumn: character?.column ?? 4,
      characterRow: character?.row ?? 4, characterScale: character?.scale ?? 1,
      characterFacing: character?.facing ?? 'S', house}};
  }
  let pendingImport = null, importGeneration = 0;
  function cancelImport() { pendingImport = null; $('world-import-confirm').hidden = true; }
  function applyImport(replace) {
    if (!pendingImport || storageError) return;
    const {world, version} = pendingImport;
    // Re-read immediately before a destructive write, including changes from other tabs.
    storageError = ''; const latest = readState();
    if (storageError) { state = latest; render(); cancelImport(); announce(storageError); return; }
    state = latest;
    const existing = state.worlds.find(w => w.id === world.id);
    if (state.worlds.some(w => w.id !== world.id && w.name.trim().toLocaleLowerCase() === world.name.trim().toLocaleLowerCase())
      || world.name.trim().toLocaleLowerCase() === 'base world') {
      cancelImport(); render(); announce('Nama world sudah digunakan oleh world lain. Data tidak diubah.'); return;
    }
    if (existing && !replace) {
      $('world-import-prompt').textContent = `World “${existing.name}” sudah ada dengan ID yang sama. Ganti isinya dengan data dari berkas?`;
      $('world-import-confirm').hidden = false; return;
    }
    if (existing && Object.keys(existing).some(k => !['id','name','description','color','showCharacter','characterColumn','characterRow','characterScale','characterFacing','house'].includes(k))) {
      cancelImport(); render(); announce('World yang ada memiliki data yang belum didukung. Data tidak diubah.'); return;
    }
    if (existing && ((version < 3 && getCharacter(existing).visible)
      || (version < 4 && getCharacter(existing).visible && getCharacter(existing).facing !== 'S')
      || (version < 5 && getHouse(existing))
      || (version === 5 && getHouse(existing) && (getHouse(existing).column !== 0 || getHouse(existing).row !== 0
        || getHouse(existing).scale !== 1 || getHouse(existing).facing !== 'S' || !getHouse(existing).visible)))) {
      cancelImport(); render(); announce('Berkas versi lama tidak dapat mengganti world ini karena dapat menghapus data objek.'); return;
    }
    const worlds = existing ? state.worlds.map(w => w.id === world.id ? world : w) : [...state.worlds, world];
    if (commit({worlds, activeId: world.id})) {
      cancelImport(); hideEditor(); announce(`${world.name} diimpor dan dibuka.`);
    } else { render(); announce(storageError || 'World tidak dapat disimpan. Data lama tidak diubah.'); }
  }
  $('world-import-file').addEventListener('change', event => {
    const file = event.target.files?.[0], generation = ++importGeneration;
    event.target.value = ''; cancelImport();
    if (!file) return;
    if (storageError) { announce(storageError); return; }
    if (file.size > 65536) { announce('Berkas terlalu besar (maksimal 64 KiB).'); return; }
    const reader = new FileReader();
    reader.onload = () => {
      if (generation !== importGeneration) return;
      try { pendingImport = parseImport(reader.result); applyImport(false); }
      catch (error) { cancelImport(); announce('Berkas tidak valid atau berisi data yang belum didukung. Data lama tidak diubah.'); }
    };
    reader.onerror = () => { if (generation === importGeneration) announce('Berkas tidak dapat dibaca.'); };
    reader.readAsText(file, 'UTF-8');
  });
  $('world-import-replace').addEventListener('click', () => applyImport(true));
  $('world-import-cancel').addEventListener('click', () => { cancelImport(); announce('Impor dibatalkan.'); });
  function button(text, action, id, className) {
    const element = document.createElement('button');
    element.type = 'button'; element.textContent = text; element.dataset.action = action;
    element.dataset.id = id; element.className = className;
    return element;
  }
  function render() {
    const active = activeWorld();
    activeLabel.textContent = storageError ? `World: ${displayName(active)} · Data bermasalah` : `World: ${displayName(active)}`;
    $('world-backup').hidden = !storageError;
    $('world-create-home').hidden = Boolean(storageError) || state.worlds.some(w => w.name.trim().toLocaleLowerCase() === 'home');
    const storageInfo = $('world-storage-info');
    try {
      const raw = localStorage.getItem(KEY);
      const description = raw === null ? 'Belum ada data world pada alamat ini.'
        : storageError ? 'Data ada, tetapi tidak seluruhnya dapat dibaca.'
        : `Data tersedia: ${state.worlds.length} world buatan.`;
      storageInfo.textContent = `${description} Alamat penyimpanan: ${location.origin}`;
    } catch (_) {
      storageInfo.textContent = 'Penyimpanan browser tidak dapat diakses. Data lama tidak diubah.';
    }
    list.replaceChildren();
    for (const world of [...BUILT_INS, ...state.worlds]) {
      const row = document.createElement('article');
      row.className = 'world-card' + (world.id === active.id ? ' is-active' : '');
      const swatch = document.createElement('span');
      swatch.className = 'world-swatch'; swatch.style.backgroundColor = world.color || '#a8c795';
      swatch.setAttribute('aria-hidden', 'true');
      const details = document.createElement('div'); details.className = 'world-card-details';
      const name = document.createElement('strong'); name.textContent = displayName(world);
      const note = document.createElement('p'); note.textContent = world.description || 'World turunan Base World';
      details.append(name, note);
      const actions = document.createElement('div'); actions.className = 'world-card-actions';
      if (world.id === active.id) {
        const current = document.createElement('span'); current.className = 'world-current'; current.textContent = 'Aktif';
        actions.append(current);
      } else actions.append(button('Buka', 'open', world.id, 'world-small'));
      if (!BUILT_INS.includes(world)) {
        const index = state.worlds.findIndex(item => item.id === world.id);
        const up = button('↑', 'move-up', world.id, 'world-small world-reorder');
        up.disabled = index === 0;
        up.setAttribute('aria-label', `Naikkan ${world.name}`);
        up.title = 'Pindah ke atas';
        const down = button('↓', 'move-down', world.id, 'world-small world-reorder');
        down.disabled = index === state.worlds.length - 1;
        down.setAttribute('aria-label', `Turunkan ${world.name}`);
        down.title = 'Pindah ke bawah';
        actions.append(up, down);
        actions.append(button('Ekspor', 'export', world.id, 'world-small'));
        const qr = button('Ekspor QR', 'export-qr', world.id, 'world-small');
        qr.setAttribute('aria-label', `Ekspor ${world.name} sebagai gambar QR`);
        actions.append(qr);
        actions.append(button('Edit', 'edit', world.id, 'world-small'));
        if (pendingDelete === world.id) {
          actions.append(button('Ya, hapus', 'confirm-delete', world.id, 'world-small world-danger'));
          actions.append(button('Batal', 'cancel-delete', world.id, 'world-small'));
        } else actions.append(button('Hapus', 'delete', world.id, 'world-small world-danger'));
      }
      row.append(swatch, details, actions); list.append(row);
    }
  }
  function hideEditor() { editor.hidden = true; editingId = null; form.reset(); }
  function showEditor(world) {
    pendingDelete = null;
    editingId = world?.id || null;
    $('world-editor-title').textContent = world ? 'Edit world' : 'World baru';
    $('world-name').value = world?.name || '';
    $('world-description').value = world?.description || '';
    $('world-color').value = world?.color || '#a8c795';
    // Existing Home worlds receive a non-destructive default; saving makes it explicit.
    $('world-character').checked = world ? (world.showCharacter ?? world.name.trim().toLocaleLowerCase() === 'home') : false;
    editor.hidden = false; announce(''); render(); $('world-name').focus();
  }
  $('world-button').addEventListener('click', () => {
    pendingDelete = null; cancelImport(); hideEditor(); render(); announce(storageError);
    if (!dialog.open) dialog.showModal();
  });
  $('world-close').addEventListener('click', () => dialog.close());
  $('world-backup').addEventListener('click', () => {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw === null) { announce('Tidak ada data world di penyimpanan alamat browser ini.'); return; }
      const url = URL.createObjectURL(new Blob([raw], {type: 'application/json'}));
      const link = document.createElement('a'); link.href = url; link.download = 'gtlauncher-worlds-backup.json';
      document.body.append(link); link.click(); link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      announce('Cadangan data asli diunduh. Data di browser tidak diubah.');
    } catch (_) { announce('Cadangan tidak dapat dibaca dari penyimpanan browser.'); }
  });
  $('world-new').addEventListener('click', () => showEditor(null));
  $('world-create-home').addEventListener('click', () => {
    // This explicit action creates a new world; never replace an existing Home or unreadable data.
    if (storageError || state.worlds.some(w => w.name.trim().toLocaleLowerCase() === 'home')) {
      announce('Home sudah ada atau penyimpanan perlu diperiksa. Data tidak diubah.'); return;
    }
    const id = globalThis.crypto?.randomUUID?.() || `world-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const home = {id, name: 'Home', description: '', color: '#a8c795',
      showCharacter: true, characterColumn: 4, characterRow: 4, characterScale: 1,
      characterFacing: 'S', house: {column: 0, row: 0}};
    if (commit({worlds: [...state.worlds, home], activeId: id})) {
      hideEditor(); announce('Home baru dibuat dan dibuka. Karakter di E5; rumah di A1–C3.');
    }
  });
  $('world-cancel').addEventListener('click', () => { hideEditor(); announce(''); });
  list.addEventListener('click', event => {
    const target = event.target.closest('button[data-action]');
    if (!target || !list.contains(target)) return;
    const {action, id} = target.dataset;
    const world = state.worlds.find(item => item.id === id);
    if (action === 'open' && (BUILT_INS.some(item => item.id === id) || world)) {
      if (commit({...state, activeId: id})) { hideEditor(); announce(`${displayName(activeWorld())} dibuka.`); }
    } else if ((action === 'move-up' || action === 'move-down') && world) {
      const from = state.worlds.findIndex(item => item.id === id);
      const to = from + (action === 'move-up' ? -1 : 1);
      if (to < 0 || to >= state.worlds.length) return;
      const worlds = state.worlds.slice();
      [worlds[from], worlds[to]] = [worlds[to], worlds[from]];
      if (commit({...state, worlds})) {
        announce(`${world.name} dipindahkan ke urutan ${to + 1} dari ${worlds.length} world buatan.`);
        // Rendering replaces the button; restore keyboard focus without constructing a selector
        // from user-supplied names or identifiers.
        const buttons = Array.from(list.querySelectorAll('button[data-action]'));
        const same = buttons.find(item => item.dataset.id === id && item.dataset.action === action && !item.disabled);
        const other = buttons.find(item => item.dataset.id === id &&
          (item.dataset.action === 'move-up' || item.dataset.action === 'move-down') && !item.disabled);
        (same || other)?.focus();
      }
    } else if ((action === 'export' || action === 'export-qr') && world) {
      // Stable world ID and the supported character object are portable to Android.
      if (!/^[a-zA-Z0-9_-]{1,128}$/.test(world.id) || world.id === 'base') {
        announce('Identitas world tidak dapat diekspor; data tidak diubah.'); return;
      }
      const character = getCharacter(world);
      const objects = character.visible ? [{type: 'character', column: character.column,
        row: character.row, scale: character.scale, facing: character.facing}] : [];
      const house = getHouse(world);
      if (house) objects.push({type: 'house', ...house});
      const payload = {format: 'gtlauncher-world', version: house ? 6 : 4,
        world: {id: world.id, name: world.name, description: world.description,
          color: world.color, objects}};
      const safeName = world.name.replace(/[^a-z0-9_-]+/gi, '-').replace(/^-+|-+$/g, '') || 'world';
      if (action === 'export-qr') {
        if (!window.WorldQRCode) { announce('Pembuat QR tidak tersedia. Gunakan ekspor JSON.'); return; }
        try {
          // Compact JSON maximises QR capacity. Never truncate data to make it fit.
          const canvas = document.createElement('canvas');
          window.WorldQRCode.toCanvas(canvas, JSON.stringify(payload),
            {errorCorrectionLevel: 'M', width: 1200, margin: 4}, error => {
              if (error) { announce('Data terlalu besar atau QR tidak dapat dibuat. Gunakan ekspor JSON.'); return; }
              try {
                const dataUrl = canvas.toDataURL('image/png');
                showExport(dataUrl, `gtlauncher-${safeName}-QR.png`, true);
                announce('QR siap di bawah daftar world. Unduh atau simpan gambarnya, lalu unggah ke chat.');
              } catch (_) { announce('Gambar QR tidak dapat diunduh. Gunakan ekspor JSON.'); }
            });
        } catch (_) { announce('Data terlalu besar atau QR tidak dapat dibuat. Gunakan ekspor JSON.'); }
      } else {
        const json = JSON.stringify(payload, null, 2);
        const blob = new Blob([json], {type: 'application/json'});
        const url = URL.createObjectURL(blob);
        showExport(url, `gtlauncher-${safeName}.json`, false, json);
        announce('JSON siap di bawah daftar world. Tekan Unduh JSON atau pilih opsi simpan yang tersedia.');
      }
    } else if (action === 'edit' && world) showEditor(world);
    else if (action === 'delete' && world) { pendingDelete = id; render(); announce(`Hapus ${world.name}? Pilih Ya, hapus untuk memastikan.`); }
    else if (action === 'cancel-delete') { pendingDelete = null; render(); announce('Penghapusan dibatalkan.'); }
    else if (action === 'confirm-delete' && world) {
      const next = {worlds: state.worlds.filter(item => item.id !== id),
        activeId: state.activeId === id ? 'base' : state.activeId};
      if (commit(next)) { pendingDelete = null; if (editingId === id) hideEditor(); render(); announce(`${world.name} dihapus.`); }
    }
  });
  form.addEventListener('submit', event => {
    event.preventDefault();
    const name = $('world-name').value.trim();
    const description = $('world-description').value.trim();
    const color = $('world-color').value;
    if (!name || name.length > 48 || description.length > 160 || !/^#[0-9a-f]{6}$/i.test(color)) {
      announce('Periksa nama, catatan, dan warna world.'); return;
    }
    if ([BASE, ...state.worlds].some(world => world.id !== editingId && world.name.toLocaleLowerCase() === name.toLocaleLowerCase())) {
      announce('Nama world sudah digunakan. Pilih nama lain.'); $('world-name').focus(); return;
    }
    if (editingId && !state.worlds.some(world => world.id === editingId)) { announce('World tidak ditemukan.'); return; }
    const id = editingId || (globalThis.crypto?.randomUUID?.() || `world-${Date.now()}-${Math.random().toString(36).slice(2)}`);
    const existing = state.worlds.find(item => item.id === id);
    const world = {...existing, id, name, description, color, showCharacter: $('world-character').checked,
      house: existing ? (existing.house ?? null) : (name.toLocaleLowerCase() === 'home' ? {column: 0, row: 0} : null)};
    const worlds = editingId ? state.worlds.map(item => item.id === id ? world : item) : [...state.worlds, world];
    const next = {worlds, activeId: editingId ? state.activeId : id};
    const wasEditing = Boolean(editingId);
    if (commit(next)) { hideEditor(); announce(wasEditing ? 'World diperbarui.' : `${name} disimpan dan dibuka.`); }
  });
  window.addEventListener('storage', event => {
    if (event.key !== KEY) return;
    storageError = ''; state = readState(); pendingDelete = null; hideEditor(); render();
    announce(storageError || 'Daftar world diperbarui dari tab lain.');
    window.dispatchEvent(new Event('worldchange'));
  });
  window.worldManager = Object.freeze({getActive: activeWorld, getCharacter, updateCharacter, getHouse, updateHouse});
  render();
})();
