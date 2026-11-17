// Pure helpers for the future multi-level terrain format. Not yet wired into saved worlds.
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.BlockStack = factory();
})(typeof window !== 'undefined' ? window : globalThis, () => {
  'use strict';
  const MAX_HEIGHT = 32;
  const validMaterial = value => Number.isInteger(value) && value >= 1 && value <= 6;
  function valid(stack) {
    return Array.isArray(stack) && stack.length <= MAX_HEIGHT && stack.every(validMaterial);
  }
  function requireStack(stack) {
    if (!valid(stack)) throw new TypeError('Invalid block stack');
  }
  function add(stack, material) {
    requireStack(stack);
    if (!validMaterial(material)) throw new TypeError('Invalid block material');
    if (stack.length === MAX_HEIGHT) return null;
    return [...stack, material];
  }
  function replaceTop(stack, material) {
    requireStack(stack);
    if (!validMaterial(material)) throw new TypeError('Invalid block material');
    if (!stack.length) return null;
    return [...stack.slice(0, -1), material];
  }
  function removeTop(stack) {
    requireStack(stack);
    return stack.length ? stack.slice(0, -1) : null;
  }
  function canStep(from, to) {
    requireStack(from); requireStack(to);
    return from.length > 0 && to.length > 0 && Math.abs(from.length - to.length) <= 1;
  }
  return Object.freeze({MAX_HEIGHT, valid, add, replaceTop, removeTop, canStep});
});
