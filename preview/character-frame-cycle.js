// Pure full-frame gait timing, ready for a complete atlas. This module is not
// connected to the scene until all eight frames exist for each pose/direction.
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.CharacterFrameCycle = factory();
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  const FRAME_COUNT = 8, TAU = Math.PI * 2;
  function index(phase) {
    if (!Number.isFinite(phase)) return 0;
    const wrapped = ((phase % TAU) + TAU) % TAU;
    return Math.floor(wrapped / TAU * FRAME_COUNT + .5) % FRAME_COUNT;
  }
  // Never animate an incomplete sequence or accidentally reuse a missing frame.
  function frame(frames, phase) {
    if (!Array.isArray(frames) || frames.length !== FRAME_COUNT || Array.from(frames).some(value => !value)) return null;
    return frames[index(phase)];
  }
  return Object.freeze({FRAME_COUNT,index,frame});
});
