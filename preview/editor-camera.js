// Preview-only camera. It never modifies world coordinates or storage.
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const stage = document.querySelector('.preview-stage'), canvas = $('sky-canvas');
  const pan = $('editor-pan'), value = $('editor-zoom-value'), surface = $('editor-pan-surface');
  const scene = window.sceneEditing;
  if (!stage || !canvas || !pan || !value || !surface || !scene?.camera || !scene?.setCamera) return;
  let panning = false, drag = null, lastWorld = window.worldManager?.getActive()?.id;
  function update() {
    const {zoom} = scene.camera();
    value.value = `${Math.round(zoom * 100)}%`;
    value.textContent = value.value;
    $('editor-zoom-in').disabled = zoom >= 5;
    $('editor-zoom-out').disabled = zoom <= 0.1;
    pan.setAttribute('aria-pressed', String(panning));
    const active = panning && stage.dataset.mode === 'editor';
    surface.hidden = !active;
    canvas.classList.toggle('is-panning', active);
  }
  function reset() { scene.setCamera(1, 0, 0); update(); }
  function changeZoom(amount) {
    const c = scene.camera();
    const zoom = Math.round((c.zoom + amount) * 10) / 10;
    scene.setCamera(zoom, c.panX, c.panY); update();
  }
  pan.addEventListener('click', () => { panning = !panning; drag = null; update(); });
  $('editor-zoom-in').addEventListener('click', () => changeZoom(0.1));
  $('editor-zoom-out').addEventListener('click', () => changeZoom(-0.1));
  $('editor-fit').addEventListener('click', reset);
  $('mode-gameplay').addEventListener('click', () => { panning = false; drag = null; scene.setMode?.('gameplay'); update(); });
  $('mode-map-editor').addEventListener('click', () => { scene.setMode?.('editor'); update(); });
  window.addEventListener('worldchange', () => {
    const id = window.worldManager?.getActive()?.id;
    if (id !== lastWorld) { lastWorld = id; drag = null; if (stage.dataset.mode === 'editor') reset(); else update(); }
  });
  surface.addEventListener('pointerdown', event => {
    if (stage.dataset.mode !== 'editor' || !panning || drag || (event.pointerType === 'mouse' && event.button !== 0)) return;
    const rect = canvas.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    const camera = scene.camera();
    drag = {id:event.pointerId, x:event.clientX, y:event.clientY, camera, width:rect.width, height:rect.height};
    try { surface.setPointerCapture(event.pointerId); } catch (_) { /* pointer capture may be unavailable */ }
    event.preventDefault(); event.stopImmediatePropagation();
  }, true);
  surface.addEventListener('pointermove', event => {
    if (!drag || drag.id !== event.pointerId) return;
    const dx = (event.clientX-drag.x) * 320 / drag.width;
    const dy = (event.clientY-drag.y) * 200 / drag.height;
    scene.setCamera(drag.camera.zoom, drag.camera.panX+dx, drag.camera.panY+dy);
    event.preventDefault(); event.stopImmediatePropagation();
  }, true);
  function end(event) {
    if (!drag || drag.id !== event.pointerId) return;
    drag = null; event.preventDefault(); event.stopImmediatePropagation();
  }
  surface.addEventListener('pointerup', end, true);
  surface.addEventListener('pointercancel', end, true);
  surface.addEventListener('lostpointercapture', event => { if (drag && drag.id === event.pointerId) drag = null; });
  update();
})();
