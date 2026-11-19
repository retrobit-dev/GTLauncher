(() => {
  'use strict';
  const KEY = 'gtlauncher.worlds.v1';
  const HOME_BACKUP_PREFIX = 'gtlauncher.home-before-25.';
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
  let loadedRaw = null;
  const validTile = t => Number.isInteger(t) && t >= 0 && t <= 10;
  const WORLD_KEYS = ['id','name','description','color','showCharacter','characterColumn','characterRow','characterScale','characterFacing','house','gridSize','tiles','blockStacks','orangeTrees','fences','bridges'];
  // The top block mirrors tiles for legacy flat-world consumers. A missing
  // stack layer means every non-empty tile has one block; no migration is needed.
  function validStacks(stacks, tiles, size) {
    return Array.isArray(stacks) && Array.isArray(tiles) && stacks.length === size * size &&
      stacks.every((stack,i) => Array.isArray(stack) && stack.length <= 32 &&
        stack.every(t => Number.isInteger(t) && t >= 1 && t <= 10) &&
        (stack.length ? stack[stack.length-1] : 0) === tiles[i]);
  }
  function validOrangeTrees(trees, size) {
    if(trees===undefined) return true;
    if(!Array.isArray(trees) || trees.length>size*size) return false;
    const occupied=new Set(), ids=new Set();
    for(const tree of trees) {
      if(!tree || Object.keys(tree).some(k=>!['column','row','facing','kind','size','variant','plantId','plantedAt'].includes(k)) || (tree.facing!==undefined && !['N','E','S','W'].includes(tree.facing)) || (tree.kind!==undefined && !['conifer','wild-grass','bush'].includes(tree.kind)) || (tree.size!==undefined && (!['conifer','wild-grass','bush'].includes(tree.kind) || (tree.size!==1 && !(tree.kind==='bush' && tree.size===2)))) || (['wild-grass','bush'].includes(tree.kind) && tree.size===undefined) || (tree.variant!==undefined && (tree.kind!=='bush' || !['flower','berry'].includes(tree.variant))) || !Number.isInteger(tree.column) || !Number.isInteger(tree.row) || tree.column<0 || tree.row<0 || tree.column+(tree.size||2)>size || tree.row+(tree.size||2)>size) return false;
      if(tree.plantId!==undefined || tree.plantedAt!==undefined) {
        if(tree.kind!=='bush' || !['flower','berry'].includes(tree.variant) || typeof tree.plantId!=='string' || !/^[a-zA-Z0-9_-]{1,80}$/.test(tree.plantId) || ids.has(tree.plantId) || !Number.isSafeInteger(tree.plantedAt) || tree.plantedAt<0 || tree.plantedAt>253402300799000) return false;
        ids.add(tree.plantId);
      }
      for(let r=tree.row;r<tree.row+(tree.size||2);r++) for(let c=tree.column;c<tree.column+(tree.size||2);c++) { const i=r*size+c; if(occupied.has(i)) return false; occupied.add(i); }
    }
    return true;
  }
  function validFences(fences,world,tiles=world.tiles,stacks=world.blockStacks) {
    if(fences===undefined)return true;
    const size=world.gridSize;
    if(!Array.isArray(fences) || !Array.isArray(tiles) || fences.length>size*size*2+size*2)return false;
    const seen=new Set();
    for(const f of fences) {
      if(!f || typeof f!=='object' || Array.isArray(f) || Object.keys(f).some(k=>!['column','row','edge','material'].includes(k)) || !Number.isInteger(f.column) || !Number.isInteger(f.row) || f.column<0 || f.row<0 || f.column>=size || f.row>=size || !['N','E','S','W'].includes(f.edge) || !['wood','stone'].includes(f.material))return false;
      const i=f.row*size+f.column,key=window.FenceEdges.key(f);
      const h=world.house;
      if(!tiles[i] || tiles[i]===3 || seen.has(key) || (h && f.column>=h.column && f.column<h.column+3 && f.row>=h.row && f.row<h.row+3))return false;
      seen.add(key);
    }
    return true;
  }
  function validBridges(world,bridges=world.bridges) {return bridges===undefined || Boolean(window.BridgeGeometry?.valid(world,bridges));}
  function replaceBridges(id,expected,next) {
    const world=activeWorld();
    if(id==='base' || world.id!==id || JSON.stringify(world.bridges||[])!==JSON.stringify(expected) || !validBridges(world,next))return false;
    return commit({...state,worlds:state.worlds.map(w=>{if(w.id!==id)return w;const copy={...w};if(next.length)copy.bridges=next.map(b=>({...b}));else delete copy.bridges;return copy;})});
  }
  function replaceFences(id,expected,next) {
    const world=activeWorld();
    if(id==='base' || world.id!==id || JSON.stringify(world.fences||[])!==JSON.stringify(expected) || !validFences(next,world) || !validBridges({...world,fences:next}))return false;
    return commit({...state,worlds:state.worlds.map(w=>{if(w.id!==id)return w;const copy={...w};if(next.length)copy.fences=next.map(f=>({...f}));else delete copy.fences;return copy;})});
  }
  // Replace both object collections in one storage transaction; used by group edits and history.
  function replaceObjectGroup(id,expected,next) {
    const world=activeWorld(),size=world.gridSize;
    if(id==='base' || world.id!==id || !Array.isArray(world.tiles) ||
      JSON.stringify(world.orangeTrees||[])!==JSON.stringify(expected.trees) || JSON.stringify(world.fences||[])!==JSON.stringify(expected.fences) ||
      !validOrangeTrees(next.trees,size) || !validFences(next.fences,world) || !validBridges({...world,orangeTrees:next.trees,fences:next.fences})) return false;
    const candidate={...world,orangeTrees:next.trees},player=getCharacter(world),house=getHouse(world);
    if(!validBridges(candidate) || !validTreeGround(candidate,world.tiles) || (player.visible && (solidTreeAt(candidate,player.column,player.row) || (treeAt(candidate,player.column,player.row) && !treeAt(world,player.column,player.row)))))return false;
    if(house)for(let r=house.row;r<house.row+3;r++)for(let c=house.column;c<house.column+3;c++)if(treeAt(candidate,c,r))return false;
    return commit({...state,worlds:state.worlds.map(w=>{if(w.id!==id)return w;const copy={...w};if(next.trees.length)copy.orangeTrees=next.trees.map(t=>({...t}));else delete copy.orangeTrees;if(next.fences.length)copy.fences=next.fences.map(f=>({...f}));else delete copy.fences;return copy;})});
  }
  function treeAt(world,column,row) { return (world.orangeTrees||[]).some(t=>column>=t.column && column<t.column+(t.size||2) && row>=t.row && row<t.row+(t.size||2)); }
  function solidTreeAt(world,column,row) { return (world.orangeTrees||[]).some(t=>t.kind!=='wild-grass' && t.kind!=='bush' && column>=t.column && column<t.column+(t.size||2) && row>=t.row && row<t.row+(t.size||2)); }
  function validTreeGround(world, tiles, stacks=world.blockStacks) {
    const size=world.gridSize;
    for(const t of world.orangeTrees||[]) {
      const height=i=>stacks ? stacks[i].length : 1, first=height(t.row*size+t.column);
      for(let r=t.row;r<t.row+(t.size||2);r++) for(let c=t.column;c<t.column+(t.size||2);c++) {const i=r*size+c;if(!tiles[i] || tiles[i]===3 || height(i)!==first)return false;}
    }
    return true;
  }
  let exportUrl = null;
  let exportFile = null;
  function showExport(url, filename, qr, json = '', previewOnly = false) {
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
      : previewOnly ? 'JSON ini dapat diimpor di preview dan Android versi terbaru. Peta disimpan terpisah pada masing-masing perangkat.'
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

  function validHouse(h, size = 9) {
    return h === null || (h && typeof h === 'object' && !Array.isArray(h)
      && Object.keys(h).every(k => ['column', 'row', 'scale', 'facing', 'visible'].includes(k))
      && Number.isInteger(h.column) && h.column >= 0 && h.column <= size - 3
      && Number.isInteger(h.row) && h.row >= 0 && h.row <= size - 3
      && (h.scale === undefined || (Number.isFinite(h.scale) && h.scale >= 0.5 && h.scale <= 1.5))
      && (h.facing === undefined || ['N', 'E', 'S', 'W'].includes(h.facing))
      && (h.visible === undefined || typeof h.visible === 'boolean'));
  }
  function readState() {
    try {
      const raw = localStorage.getItem(KEY);
      loadedRaw = raw;
      if (raw === null) return {worlds: [], activeId: 'base'};
      const value = JSON.parse(raw);
      if (!value || !Array.isArray(value.worlds) || typeof value.activeId !== 'string') throw new Error('format');
      if (Object.keys(value).some(k => !['worlds','activeId'].includes(k))) throw new Error('unsupported fields');
      const ids = new Set();
      const readableWorlds = [];
      let skipped = 0;
      for (const world of value.worlds) {
        if (!world || typeof world !== 'object' || Array.isArray(world) || Object.keys(world).some(k => !WORLD_KEYS.includes(k)) || typeof world.id !== 'string' || !world.id || world.id === 'base' || ids.has(world.id)
          || typeof world.name !== 'string' || !world.name.trim() || world.name.length > 48
          || typeof world.description !== 'string' || world.description.length > 160
          || (world.gridSize !== undefined && ![20,25,50,100].includes(world.gridSize))
          || ([20,25,50,100].includes(world.gridSize) && world.tiles === undefined)
          || (world.tiles !== undefined && (![20,25,50,100].includes(world.gridSize) || !Array.isArray(world.tiles) || world.tiles.length !== world.gridSize * world.gridSize || world.tiles.some(t => !validTile(t))))
          || (world.blockStacks !== undefined && !validStacks(world.blockStacks, world.tiles, world.gridSize))
          || (world.house !== undefined && !validHouse(world.house, world.gridSize || 9))
          || !validOrangeTrees(world.orangeTrees,world.gridSize || 9)
          || !validFences(world.fences,world) || !validBridges(world)
          || (world.showCharacter !== undefined && typeof world.showCharacter !== 'boolean')
          || (world.characterFacing !== undefined && !['N', 'E', 'S', 'W'].includes(world.characterFacing))
          || (world.characterColumn !== undefined && (!Number.isInteger(world.characterColumn) || world.characterColumn < 0 || world.characterColumn >= (world.gridSize || 9)))
          || (world.characterRow !== undefined && (!Number.isInteger(world.characterRow) || world.characterRow < 0 || world.characterRow >= (world.gridSize || 9)))
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
  // Home is upgraded only when the complete saved state is readable; a copy of
  // the original storage is kept before any write, including terrain holes.
  function upgradeHome25() {
    if (storageError || loadedRaw === null) return false;
    const home = state.worlds.find(w => w.name.trim().toLocaleLowerCase() === 'home' && ![25,50,100].includes(w.gridSize));
    if (!home || home.blockStacks) return false;
    const oldSize = home.gridSize || 9, tiles = Array(625).fill(1);
    if (home.tiles) for (let r=0;r<oldSize;r++) for (let c=0;c<oldSize;c++) tiles[r*25+c]=home.tiles[r*oldSize+c];
    try {
      if (localStorage.getItem(KEY) !== loadedRaw) { storageError='Data world berubah di tab lain. Muat ulang sebelum mengedit; data tidak ditimpa.'; return false; }
      const backupKey=HOME_BACKUP_PREFIX + Date.now() + '-' + Math.random().toString(36).slice(2);
      localStorage.setItem(backupKey,loadedRaw);
      if (localStorage.getItem(backupKey)!==loadedRaw) throw new Error('backup failed');
      const next={...state,worlds:state.worlds.map(w=>w===home?{...w,gridSize:25,tiles}:w)};
      if(localStorage.getItem(KEY)!==loadedRaw) { storageError='Data world berubah di tab lain. Muat ulang sebelum mengedit; data tidak ditimpa.'; return false; }
      const raw=JSON.stringify(next);localStorage.setItem(KEY,raw);loadedRaw=raw;state=next;return true;
    } catch (_) { storageError='Home belum dapat diperbarui. Data lama tidak diubah; periksa ruang penyimpanan browser.'; return false; }
  }
  const homeUpgraded = upgradeHome25();

  function activeWorld() { return BUILT_INS.find(world => world.id === state.activeId)
    || state.worlds.find(world => world.id === state.activeId) || BASE; }
  function getCharacter(world = activeWorld()) {
    return {visible: world.id !== 'base' && (world.showCharacter ?? world.name.trim().toLocaleLowerCase() === 'home'),
      column: world.characterColumn ?? 4, row: world.characterRow ?? 4,
      scale: world.characterScale ?? 1, facing: world.characterFacing ?? 'S'};
  }
  function updateCharacter(id, config) {
    if (id === 'base' || activeWorld().id !== id || !state.worlds.some(world => world.id === id)
      || !config || (activeWorld().tiles && !activeWorld().tiles[config.row * activeWorld().gridSize + config.column])
      || typeof config.visible !== 'boolean' || !Number.isInteger(config.column) || config.column < 0 || config.column >= (activeWorld().gridSize || 9)
      || !Number.isInteger(config.row) || config.row < 0 || config.row >= (activeWorld().gridSize || 9)
      || !Number.isFinite(config.scale) || config.scale < 0.5 || config.scale > 1.5
      || !['N', 'E', 'S', 'W'].includes(config.facing) || (config.visible && solidTreeAt(activeWorld(),config.column,config.row))) return false;
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
  function levelHouse(world, house) {
    if (!house || !world.blockStacks) return true;
    const size=world.gridSize, level=world.blockStacks[house.row*size+house.column]?.length;
    for(let r=house.row;r<house.row+3;r++) for(let c=house.column;c<house.column+3;c++)
      if(world.blockStacks[r*size+c]?.length!==level) return false;
    return Boolean(level);
  }
  function updateHouse(id, house) {
    if (id === 'base' || activeWorld().id !== id || !state.worlds.some(w => w.id === id && w.house)
      || !house || !validBridges({...activeWorld(),house}) || !validFences(activeWorld().fences,{...activeWorld(),house}) || Array.from({length:9},(_,i)=>treeAt(activeWorld(),house.column+i%3,house.row+Math.floor(i/3))).some(Boolean) || !validHouse(house, activeWorld().gridSize || 9) || !levelHouse(activeWorld(),house) || (activeWorld().tiles && Array.from({length: 9}, (_, i) => activeWorld().tiles[(house.row + Math.floor(i / 3)) * activeWorld().gridSize + house.column + i % 3]).some(t => !t)) || typeof house.visible !== 'boolean'
      || typeof house.scale !== 'number' || !['N','E','S','W'].includes(house.facing)) return false;
    return commit({...state, worlds: state.worlds.map(w => w.id === id ? {...w, house: {...house}} : w)});
  }
  // Single player house per custom world. Used by catalogue placement and its undo/redo.
  function replacePlayerHouse(id, expected, next) {
    const world=activeWorld(), current=getHouse(world), size=world.gridSize;
    if(id==='base' || world.id!==id || ![20,25,50,100].includes(size) || !Array.isArray(world.tiles)
      || JSON.stringify(current)!==JSON.stringify(expected) || (next!==null && (!validHouse(next,size)
      || typeof next.visible!=='boolean' || typeof next.scale!=='number' || !['N','E','S','W'].includes(next.facing)))) return false;
    if(next) {
      const player=getCharacter(world);
      if(player.visible && player.column>=next.column && player.column<next.column+3 && player.row>=next.row && player.row<next.row+3) return false;
      if(!levelHouse(world,next) || !validBridges({...world,house:next}) || !validFences(world.fences,{...world,house:next})) return false;
      for(let r=next.row;r<next.row+3;r++) for(let c=next.column;c<next.column+3;c++) if(treeAt(world,c,r))return false;
      for(let r=next.row;r<next.row+3;r++) for(let c=next.column;c<next.column+3;c++)
        if(!world.tiles[r*size+c] || world.tiles[r*size+c]===3) return false;
    }
    return commit({...state,worlds:state.worlds.map(w=>w.id===id ? {...w,house:next ? {...next} : null} : w)});
  }
  function replaceOrangeTrees(id, expected, next) {
    const world=activeWorld(), size=world.gridSize;
    if(id==='base' || world.id!==id || ![20,25,50,100].includes(size) || !Array.isArray(world.tiles) || JSON.stringify(world.orangeTrees||[])!==JSON.stringify(expected) || !validOrangeTrees(next,size))return false;
    const candidate={...world,orangeTrees:next}, player=getCharacter(world), house=getHouse(world);
    if(!validBridges(candidate) || !validTreeGround(candidate,world.tiles) || (player.visible && (solidTreeAt(candidate,player.column,player.row) || (treeAt(candidate,player.column,player.row) && !treeAt(world,player.column,player.row)))))return false;
    if(house) for(let r=house.row;r<house.row+3;r++)for(let c=house.column;c<house.column+3;c++) if(treeAt(candidate,c,r))return false;
    return commit({...state,worlds:state.worlds.map(w=>{if(w.id!==id)return w;const updated={...w};if(next.length)updated.orangeTrees=next.map(t=>({...t}));else delete updated.orangeTrees;return updated;})});
  }
  function displayName(world) { return world.name; }
  function announce(text) { message.textContent = text; }
  function commit(next) {
    if (storageError) { announce(storageError); return false; }
    try {
      if (localStorage.getItem(KEY) !== loadedRaw) {
        storageError = 'Data world berubah di tab lain. Muat ulang sebelum mengedit; data tidak ditimpa.';
        announce(storageError); return false;
      }
      const raw = JSON.stringify(next); localStorage.setItem(KEY, raw); loadedRaw = raw;
    }
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
    if (keys(root, ['format','version','world']) && root.format === 'gtlauncher-preview-world' && [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15].includes(root.version)) {
      const w = root.world;
      if (!keys(w, WORLD_KEYS) || ![20,25,50,100].includes(w.gridSize) || !Array.isArray(w.tiles) || w.tiles.length !== w.gridSize * w.gridSize
        || w.tiles.some(t => !validTile(t) || (t > (root.version === 1 ? 1 : root.version === 2 ? 5 : root.version < 14 ? 6 : 10)))
        || (root.version < 5 && w.orangeTrees !== undefined) || (root.version < 6 && w.orangeTrees?.some(t=>t.facing!==undefined)) || (root.version < 7 && w.orangeTrees?.some(t=>t.kind!==undefined)) || (root.version < 8 && w.orangeTrees?.some(t=>t.size!==undefined)) || (root.version < 9 && w.orangeTrees?.some(t=>t.kind==='wild-grass')) || (root.version < 10 && w.orangeTrees?.some(t=>t.kind==='bush')) || (root.version < 11 && w.orangeTrees?.some(t=>t.kind==='bush' && (t.size===2 || t.variant!==undefined))) || (root.version < 12 && w.orangeTrees?.some(t=>t.plantId!==undefined || t.plantedAt!==undefined)) || !validOrangeTrees(w.orangeTrees,w.gridSize)
        || (root.version < 13 && w.fences !== undefined) || !validFences(w.fences,w)
        || (root.version < 15 && w.bridges !== undefined) || !validBridges(w)
        || (root.version < 14 && Array.isArray(w.blockStacks) && w.blockStacks.some(stack=>Array.isArray(stack) && stack.some(t=>t>6)))
        || (root.version < 4 && w.blockStacks !== undefined)
        || (root.version === 4 && !validStacks(w.blockStacks, w.tiles, w.gridSize)) || (root.version >= 5 && w.blockStacks !== undefined && !validStacks(w.blockStacks,w.tiles,w.gridSize)) || typeof w.id !== 'string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(w.id) || w.id === 'base'
        || typeof w.name !== 'string' || !w.name.trim() || w.name.length > 48 || typeof w.description !== 'string' || w.description.length > 160
        || typeof w.color !== 'string' || !/^#[0-9a-f]{6}$/i.test(w.color)
        || typeof w.showCharacter !== 'boolean' || !Number.isInteger(w.characterColumn) || w.characterColumn < 0 || w.characterColumn >= w.gridSize
        || !Number.isInteger(w.characterRow) || w.characterRow < 0 || w.characterRow >= w.gridSize
        || !Number.isFinite(w.characterScale) || w.characterScale < .5 || w.characterScale > 1.5
        || !['N','E','S','W'].includes(w.characterFacing) || !validHouse(w.house,w.gridSize)
        || !w.tiles[w.characterRow*w.gridSize+w.characterColumn]) return fail();
      if (w.house) for (let r=w.house.row;r<w.house.row+3;r++) for (let c=w.house.column;c<w.house.column+3;c++) if (!w.tiles[r*w.gridSize+c]) return fail();
      if (!levelHouse(w,w.house) || !validTreeGround(w,w.tiles) || (w.showCharacter && solidTreeAt(w,w.characterColumn,w.characterRow))) return fail();
      if(w.house) for(let r=w.house.row;r<w.house.row+3;r++)for(let c=w.house.column;c<w.house.column+3;c++) if(treeAt(w,c,r))return fail();
      return {version: root.version===15 ? 43 : root.version===14 ? 42 : root.version===13 ? 41 : root.version===12 ? 40 : root.version===11 ? 39 : root.version===10 ? 38 : root.version===9 ? 37 : root.version===8 ? 36 : root.version===7 ? 35 : root.version===6 ? 34 : root.version===5 ? 33 : 32, world: {...w, tiles: w.tiles.slice(), house: w.house ? {...w.house} : null}};
    }
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
    if(existing && version!==43 && (existing.bridges||[]).length) {cancelImport();render();announce('Berkas lama tidak mendukung jembatan; data tidak diubah.');return;}
    if(existing && version!==43 && version!==42 && ((existing.tiles||[]).some(t=>t>6) || (existing.blockStacks||[]).some(stack=>stack.some(t=>t>6)))) {cancelImport();render();announce('Berkas lama tidak mendukung block beratap rumput; data tidak diubah.');return;}
    if (existing && version!==43 && version!==42 && version!==41 && (existing.fences||[]).length) { cancelImport(); render(); announce('Berkas lama tidak mendukung pagar; data tidak diubah.'); return; }
    if (existing && version!==33 && version!==34 && version!==35 && version!==36 && version!==37 && version!==38 && version!==39 && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).length) { cancelImport(); render(); announce('Berkas lama tidak mendukung pohon jeruk; data tidak diubah.'); return; }
    if (existing && version!==35 && version!==36 && version!==37 && version!==38 && version!==39 && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).some(t=>t.kind!==undefined)) { cancelImport(); render(); announce('Berkas lama tidak mendukung jenis pohon ini; data tidak diubah.'); return; }
    if (existing && version!==36 && version!==37 && version!==38 && version!==39 && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).some(t=>t.size!==undefined)) { cancelImport(); render(); announce('Berkas lama tidak mendukung ukuran pohon ini; data tidak diubah.'); return; }
    if (existing && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).some(t=>t.plantId!==undefined)) { cancelImport(); render(); announce('Berkas lama tidak mendukung identitas tanaman; data tidak diubah.'); return; }
    if (existing && version!==39 && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).some(t=>t.kind==='bush' && (t.size===2 || t.variant!==undefined))) { cancelImport(); render(); announce('Berkas lama tidak mendukung variasi semak; data tidak diubah.'); return; }
    if (existing && version!==38 && version!==39 && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).some(t=>t.kind==='bush')) { cancelImport(); render(); announce('Berkas lama tidak mendukung semak; data tidak diubah.'); return; }
    if (existing && version!==37 && version!==38 && version!==39 && version!==40 && version!==43 && version!==42 && version!==41 && (existing.orangeTrees||[]).some(t=>t.kind==='wild-grass')) { cancelImport(); render(); announce('Berkas lama tidak mendukung rumput liar; data tidak diubah.'); return; }
    if (existing && version===33 && (existing.orangeTrees||[]).some(t=>t.facing!==undefined)) { cancelImport(); render(); announce('Berkas lama tidak mendukung arah pohon; data tidak diubah.'); return; }
    if (existing && (version !== 32 && version !== 33 && version !== 34 && version !== 35 && version !== 36 && version !== 37 && version !== 38 && version !== 39 && version !== 40 && version !== 43 && version !== 42 && version !== 41 ? Object.keys(existing).some(k => !['id','name','description','color','showCharacter','characterColumn','characterRow','characterScale','characterFacing','house'].includes(k)) : Object.keys(existing).some(k => !WORLD_KEYS.includes(k)))) {
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
    if (file.size > 8 * 1024 * 1024) { announce('Berkas terlalu besar (maksimal 8 MiB).'); return; }
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
    $('world-backup').hidden = !storageError && !state.worlds.some(w => [20,25,50,100].includes(w.gridSize));
    try { $('world-backup-home').hidden = !Object.keys(localStorage).some(key => key.startsWith(HOME_BACKUP_PREFIX)); }
    catch (_) { $('world-backup-home').hidden = true; }
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
    $('world-character').checked = world ? (world.showCharacter ?? world.name.trim().toLocaleLowerCase() === 'home') : true;
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
  $('world-backup-home').addEventListener('click', () => {
    try {
      const key=Object.keys(localStorage).filter(k=>k.startsWith(HOME_BACKUP_PREFIX)).sort().pop();
      const raw=key && localStorage.getItem(key); if(!raw) { announce('Cadangan Home tidak ditemukan.');return; }
      const url=URL.createObjectURL(new Blob([raw],{type:'application/json'})), link=document.createElement('a');
      link.href=url;link.download='gtlauncher-home-sebelum-25.json';document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
      announce('Cadangan sebelum Home diperbarui diunduh.');
    } catch (_) { announce('Cadangan Home tidak dapat dibaca.'); }
  });
  $('world-new').addEventListener('click', () => showEditor(null));
  $('world-create-home').addEventListener('click', () => {
    // This explicit action creates a new world; never replace an existing Home or unreadable data.
    if (storageError || state.worlds.some(w => w.name.trim().toLocaleLowerCase() === 'home')) {
      announce('Home sudah ada atau penyimpanan perlu diperiksa. Data tidak diubah.'); return;
    }
    const id = globalThis.crypto?.randomUUID?.() || `world-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const home = {id, name: 'Home', description: '', color: '#a8c795', gridSize: 50, tiles: Array(2500).fill(1),
      showCharacter: true, characterColumn: 24, characterRow: 24, characterScale: 1,
      characterFacing: 'S', house: {column: 0, row: 0}};
    if (commit({worlds: [...state.worlds, home], activeId: id})) {
      hideEditor(); announce('Home 50×50 dibuat dan dibuka. Karakter di Y25; rumah di A1–C3.');
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
      if ([20,25,50,100].includes(world.gridSize)) {
        if (action === 'export-qr') { announce('QR world ini belum tersedia; gunakan ekspor JSON.'); return; }
        const character = getCharacter(world);
        const snapshot = {...world, showCharacter: character.visible, characterColumn: character.column, characterRow: character.row,
          characterScale: character.scale, characterFacing: character.facing, house: getHouse(world)};
        const json = JSON.stringify({format:'gtlauncher-preview-world',version:snapshot.bridges!==undefined ? 15 : (snapshot.tiles.some(t=>t>6) || snapshot.blockStacks?.some(stack=>stack.some(t=>t>6))) ? 14 : snapshot.fences!==undefined ? 13 : snapshot.orangeTrees?.some(t=>t.plantId!==undefined) ? 12 : snapshot.orangeTrees?.some(t=>t.kind==='bush' && (t.size===2 || t.variant!==undefined)) ? 11 : snapshot.orangeTrees?.some(t=>t.kind==='bush') ? 10 : snapshot.orangeTrees?.some(t=>t.kind==='wild-grass') ? 9 : snapshot.orangeTrees?.some(t=>t.size!==undefined) ? 8 : snapshot.orangeTrees?.some(t=>t.kind!==undefined) ? 7 : snapshot.orangeTrees?.some(t=>t.facing!==undefined) ? 6 : snapshot.orangeTrees !== undefined ? 5 : snapshot.blockStacks ? 4 : snapshot.tiles.some(t => t === 6) ? 3 : snapshot.tiles.some(t => t > 1) ? 2 : 1,world:snapshot}, null, 2);
        const blob = new Blob([json], {type:'application/json'});
        const url = URL.createObjectURL(blob);
        const safeName = world.name.replace(/[^a-z0-9_-]+/gi, '-').replace(/^-+|-+$/g, '') || 'world';
        showExport(url, `gtlauncher-preview-${safeName}.json`, false, json, true);
        const androidConifers = !snapshot.bridges && !snapshot.blockStacks && (snapshot.fences?.length || snapshot.orangeTrees?.length) && (!snapshot.orangeTrees || snapshot.orangeTrees.every(t=>t.kind==='conifer'||t.kind==='wild-grass'||t.kind==='bush'));
        if(snapshot.bridges) $('world-export-help').textContent=snapshot.blockStacks ? 'JSON jembatan bertingkat hanya dapat diimpor di preview; Android belum mendukung block bertingkat.' : 'JSON jembatan datar dapat diimpor Android versi terbaru jika semua objek didukung.';
        else if(androidConifers) $('world-export-help').textContent = 'JSON pagar/cemara/rumput/semak datar dapat diimpor Android versi terbaru. World Android dan preview tetap terpisah.';
        else if(snapshot.orangeTrees !== undefined) $('world-export-help').textContent = 'JSON pohon ini hanya dapat diimpor di preview; Android belum mendukung seluruh datanya.';
        else if(snapshot.blockStacks) $('world-export-help').textContent = 'JSON block bertingkat ini hanya dapat diimpor di preview; Android belum mendukung block bertingkat.';
        announce(snapshot.bridges ? (snapshot.blockStacks ? 'JSON jembatan bertingkat siap untuk preview.' : 'JSON jembatan datar siap untuk preview dan Android terbaru jika seluruh objek didukung.') : androidConifers ? 'JSON pagar/cemara/rumput/semak datar siap untuk preview dan Android versi terbaru.' : snapshot.orangeTrees !== undefined ? 'JSON pohon siap untuk preview; Android belum mendukung seluruh datanya.' : snapshot.blockStacks ? 'JSON block bertingkat siap untuk preview; Android belum mendukung format ini.' : 'JSON world siap untuk preview dan Android versi terbaru.'); return;
      }
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
    const world = {...existing, ...(!existing ? {gridSize: 50, tiles: Array(2500).fill(1), characterColumn: 24, characterRow: 24} : {}), id, name, description, color, showCharacter: $('world-character').checked,
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
  function updateTiles(id, tiles) {
    const world = activeWorld();
    if (id === 'base' || world.id !== id || ![20,25,50,100].includes(world.gridSize) || !Array.isArray(tiles)
      || tiles.length !== world.gridSize * world.gridSize || tiles.some(t => !validTile(t))) return false;
    // Legacy single-layer edits must never flatten a saved stack.
    if (world.blockStacks && tiles.some((t,i) => t !== world.tiles[i])) return false;
    const character = getCharacter(world), house = getHouse(world);
    if (!tiles[character.row * world.gridSize + character.column] || !validTreeGround(world,tiles) || !validFences(world.fences,world,tiles) || !validBridges({...world,tiles})) return false;
    if (house) for (let r = house.row; r < house.row + 3; r++) for (let c = house.column; c < house.column + 3; c++) if (!tiles[r * world.gridSize + c] || tiles[r * world.gridSize + c] === 3) return false;
    return commit({...state, worlds: state.worlds.map(w => w.id === id ? {...w, tiles: tiles.slice()} : w)});
  }
  function updateBlockStacks(id, stacks) {
    const world = activeWorld(), size = world.gridSize;
    if (id === 'base' || world.id !== id || ![20,25,50,100].includes(size) || !Array.isArray(stacks)) return false;
    const tiles = stacks.map(stack => Array.isArray(stack) && stack.length ? stack[stack.length-1] : 0);
    if (!validStacks(stacks, tiles, size) || !validTreeGround(world,tiles,stacks) || !validFences(world.fences,world,tiles,stacks) || !validBridges({...world,tiles,blockStacks:stacks})) return false;
    const character = getCharacter(world), house = getHouse(world);
    const oldHeight = i => world.blockStacks ? world.blockStacks[i].length : (world.tiles[i] ? 1 : 0);
    const characterIndex = character.row * size + character.column;
    if (!tiles[characterIndex] || stacks[characterIndex].length !== oldHeight(characterIndex)) return false;
    if (house) for (let r=house.row; r<house.row+3; r++) for(let c=house.column; c<house.column+3; c++) {
      const i=r*size+c;
      if (!tiles[i] || tiles[i]===3 || stacks[i].length!==oldHeight(i)) return false;
    }
    const hasElevation=stacks.some(stack=>stack.length>1);
    return commit({...state, worlds: state.worlds.map(w => {
      if (w.id!==id) return w;
      const next={...w,tiles};
      if(hasElevation) next.blockStacks=stacks.map(stack=>stack.slice());
      else delete next.blockStacks;
      return next;
    })});
  }
  function moveHouse(id, original, column, row) {
    const world=activeWorld(), house=getHouse(world), character=getCharacter(world), size=world.gridSize || 9;
    if(id==='base' || world.id!==id || !house?.visible || !original || JSON.stringify(house)!==JSON.stringify(original)
       || !Number.isInteger(column) || !Number.isInteger(row) || column<0 || row<0 || column+3>size || row+3>size)return false;
    for(let r=row;r<row+3;r++)for(let c=column;c<column+3;c++) {
      if(character.visible && character.column===c && character.row===r || treeAt(world,c,r))return false;
      if(world.tiles && (!world.tiles[r*size+c] || world.tiles[r*size+c]===3))return false;
    }
    return updateHouse(id,{...house,column,row});
  }
  function tillTile(id, column, row) {
    const world=activeWorld(), character=getCharacter(world), house=getHouse(world), size=world.gridSize;
    if(id==='base' || world.id!==id || ![20,25,50,100].includes(size) || !world.tiles || !character.visible
      || !Number.isInteger(column) || !Number.isInteger(row) || column<0 || row<0 || column>=size || row>=size
      || Math.abs(character.column-column)+Math.abs(character.row-row)!==1
      || treeAt(world,column,row)
      || (house?.visible && column>=house.column && column<house.column+3 && row>=house.row && row<house.row+3)
      || ![1,2].includes(world.tiles[row*size+column])) return false;
    const index=row*size+column;
    if (world.blockStacks) { const stacks=world.blockStacks.map(x=>x.slice());stacks[index][stacks[index].length-1]=6;return updateBlockStacks(id,stacks); }
    const tiles=world.tiles.slice();tiles[index]=6;
    return updateTiles(id,tiles);
  }
  window.worldManager = Object.freeze({getActive: activeWorld, replaceBridges, replaceObjectGroup, getCharacter, updateCharacter, getHouse, updateHouse, moveHouse, replacePlayerHouse, replaceOrangeTrees, replaceFences, updateTiles, updateBlockStacks, tillTile});
  render();
  if (homeUpgraded) announce('Home diperbarui menjadi 25×25. Cadangan sebelum perubahan tersedia untuk diunduh.');
  else if (storageError) announce(storageError);
})();
