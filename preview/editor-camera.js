// Preview-only camera. It never modifies world coordinates or storage.
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const stage = document.querySelector('.preview-stage'), canvas = $('sky-canvas');
  const pan = $('editor-pan'), value = $('editor-zoom-value'), surface = $('editor-pan-surface');
  const scene = window.sceneEditing;
  if (!stage || !canvas || !pan || !value || !surface || !scene?.camera || !scene?.setCamera) return;
  let panning = false, drag = null, suppressClick = false, lastWorld = window.worldManager?.getActive()?.id;
  function update() {
    const {zoom} = scene.camera();
    value.value = String(Math.round(zoom * 10) / 10);
    value.textContent = value.value;
    $('editor-zoom-in').disabled = zoom >= 20;
    $('editor-zoom-out').disabled = zoom <= 0.5;
    pan.setAttribute('aria-pressed', String(panning));
    const active = panning && stage.dataset.mode === 'editor';
    surface.hidden = !active;
    canvas.classList.toggle('is-panning', active);
  }
  function reset() { scene.setCamera(7.5, 0, 0); update(); }
  function changeZoom(amount) {
    const c = scene.camera();
    scene.setCamera(Math.round((c.zoom + amount) * 2) / 2, c.panX, c.panY); update();
  }
  pan.addEventListener('click', event => {
    if (suppressClick) { suppressClick = false; event.preventDefault(); return; }
    panning = !panning; drag = null; update();
  });
  $('editor-zoom-in').addEventListener('click', () => changeZoom(0.5));
  $('editor-zoom-out').addEventListener('click', () => changeZoom(-0.5));
  $('editor-fit').addEventListener('click', reset);
  $('mode-gameplay').addEventListener('click', () => { panning = false; drag = null; scene.setMode?.('gameplay'); update(); });
  $('mode-map-editor').addEventListener('click', () => { scene.setMode?.('editor'); update(); });
  window.addEventListener('worldchange', () => {
    const id = window.worldManager?.getActive()?.id;
    if (id !== lastWorld) { lastWorld = id; drag = null; if (stage.dataset.mode === 'editor') reset(); else update(); }
  });
  function begin(event, fromButton) {
    if (stage.dataset.mode !== 'editor' || (!fromButton && !panning) || drag || (event.pointerType === 'mouse' && event.button !== 0)) return;
    const rect = canvas.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    drag = {id:event.pointerId, x:event.clientX, y:event.clientY, camera:scene.camera(), width:rect.width, height:rect.height, fromButton, moved:false};
    try { event.currentTarget.setPointerCapture(event.pointerId); } catch (_) { /* global listeners handle capture failures */ }
    if (!fromButton) event.preventDefault();
    event.stopPropagation();
  }
  surface.addEventListener('pointerdown', event => begin(event, false));
  pan.addEventListener('pointerdown', event => begin(event, true));
  // Listen globally as a fallback when pointer capture is unavailable or the finger leaves the screen.
  window.addEventListener('pointermove', event => {
    if (!drag || drag.id !== event.pointerId) return;
    const dx = event.clientX-drag.x, dy = event.clientY-drag.y;
    if (!drag.moved && Math.hypot(dx,dy) < 3) return;
    drag.moved = true;
    scene.setCamera(drag.camera.zoom, drag.camera.panX+dx*320/drag.width, drag.camera.panY+dy*200/drag.height);
    event.preventDefault(); event.stopPropagation();
  }, {capture:true, passive:false});
  function end(event) {
    if (!drag || drag.id !== event.pointerId) return;
    if (drag.fromButton && drag.moved) { suppressClick = true; setTimeout(() => { suppressClick = false; }, 0); }
    drag = null;
  }
  window.addEventListener('pointerup', end, true);
  window.addEventListener('pointercancel', end, true);
  update();
})();
