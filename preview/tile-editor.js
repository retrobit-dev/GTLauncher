// Preview-only tile editing; never touches Android or legacy world storage formats.
(() => {
  'use strict';
  const stage = document.querySelector('.preview-stage'), canvas = document.getElementById('sky-canvas');
  const status = document.getElementById('tile-editor-status');
  const buttons = {object:document.getElementById('tile-object'), add:document.getElementById('tile-add'), remove:document.getElementById('tile-remove')};
  if (!stage || !canvas || !status || !buttons.object || !window.sceneEditing) return;
  let tool = 'object';
  function select(next) {
    tool = next;
    for (const [name, button] of Object.entries(buttons)) button.setAttribute('aria-pressed', String(name === tool));
    status.textContent = tool === 'object' ? '' : 'Ketuk tile pada world 20×20.';
    canvas.classList.toggle('is-editing', tool !== 'object' || document.getElementById('object-edit-button').getAttribute('aria-pressed') === 'true');
  }
  for (const [name, button] of Object.entries(buttons)) button.addEventListener('click', () => select(name));
  document.getElementById('mode-gameplay').addEventListener('click', () => select('object'));
  canvas.addEventListener('pointerdown', event => {
    if (stage.dataset.mode !== 'editor' || tool === 'object' || (event.pointerType === 'mouse' && event.button !== 0)) return;
    event.preventDefault(); event.stopImmediatePropagation();
    const world = window.worldManager.getActive();
    if (world.id === 'base' || world.gridSize !== 20 || !world.tiles) { status.textContent = 'Buka world baru 20×20; Base dan world lama tetap aman.'; return; }
    const tile = window.sceneEditing.tileAt(window.sceneEditing.toLocal(event.clientX, event.clientY));
    if (!tile) { status.textContent = 'Di luar area 20×20.'; return; }
    const next = world.tiles.slice(), index = tile.row * 20 + tile.column;
    next[index] = tool === 'add' ? 1 : 0;
    if (next[index] === world.tiles[index]) return;
    if (window.worldManager.updateTiles(world.id, next)) status.textContent = `${'ABCDEFGHIJKLMNOPQRST'[tile.column]}${tile.row+1} ${tool === 'add' ? 'ditambah' : 'dihapus'}.`;
    else status.textContent = 'Tidak tersimpan: tile digunakan objek atau penyimpanan bermasalah.';
  }, true);
})();
