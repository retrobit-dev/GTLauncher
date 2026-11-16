(() => {
  'use strict';
  const stage = document.querySelector('.preview-stage');
  const gameplay = document.getElementById('mode-gameplay');
  const editor = document.getElementById('mode-map-editor');
  const edit = document.getElementById('object-edit-button');
  if (!stage || !gameplay || !editor || !edit) return;
  let mode = 'gameplay';
  function setMode(next) {
    mode = next;
    stage.dataset.mode = mode;
    gameplay.setAttribute('aria-pressed', String(mode === 'gameplay'));
    editor.setAttribute('aria-pressed', String(mode === 'editor'));
    const editing = edit.getAttribute('aria-pressed') === 'true';
    if (mode === 'gameplay' && editing) edit.click();
    if (mode === 'gameplay' && !editing) document.getElementById('edit-status').hidden = true;
    if (mode === 'editor' && !editing) edit.click();
  }
  gameplay.addEventListener('click', () => setMode('gameplay'));
  editor.addEventListener('click', () => setMode('editor'));
  window.addEventListener('worldchange', () => {
    if (mode !== 'editor') return;
    // The editor cancels the previous world's selection first.
    if (edit.getAttribute('aria-pressed') !== 'true') edit.click();
  });
  setMode('gameplay');
})();
