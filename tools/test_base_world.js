// Dependency-free checks of the preview's actual Base World geometry and wind integration.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const sourcePath = path.join(__dirname, '..', 'preview', 'app.js');
let source = fs.readFileSync(sourcePath, 'utf8');
const marker = '  drawFrame();\n  initializeWeather();';
assert(source.includes(marker), 'preview entry point changed');
source = source.replace('    drawMoonBody(centerX, centerY, palette, moon);',
  '    window.testMoonCentre = {x: centerX, y: centerY}; drawMoonBody(centerX, centerY, palette, moon);');
source = source.replace(marker, `  window.testWorld = {
    sunPlanePosition, createCloudStates, drawCloudShadowsOnIsland, drawProjectedCloudShadows, drawMoon, objectShadowPolygon,
    geometry: () => ({cy: islandCenterY, tileHeight: islandTileHeight}),
    reset: () => { cloudEastTravel = 0; cloudNorthTravel = 0;
      cloudLastElapsed = null; cloudWindCardinal = null; }
  };`);
const rectangles = [];
const ctx = new Proxy({}, {get(target, prop) {
  if (prop in target) return target[prop];
  if (prop === 'fillRect') return (...args) => rectangles.push(args);
  return () => {};
}, set(target, prop, value) { target[prop] = value; return true; }});
const canvas = {getContext: () => ctx, addEventListener() {}};
const noop = () => {};
const window = {matchMedia: () => ({matches: false}), requestAnimationFrame: noop,
  setInterval: noop, addEventListener: noop};
const sandbox = {window, console, document: {getElementById: id => id === 'sky-canvas' ? canvas : null,
  createElement: () => canvas}, navigator: {}, performance: {now: () => 0},
  Image: function Image() {}, fetch: async () => ({ok: false}), Date, Map};
vm.createContext(sandbox);
vm.runInContext(source, sandbox, {filename: sourcePath});
const world = window.testWorld;
const {cy, tileHeight} = world.geometry();
const axis = Math.hypot(28, tileHeight);
const sun = world.sunPlanePosition(0);
const near = (a, b) => assert(Math.abs(a - b) < 1e-6, `${a} != ${b}`);
function cloud(east) {
  return {shape: ['11111','11111','11111','11111','11111'], cell: 1,
    planeX: 160 - 10 * 28 / axis + east * 14,
    planeY: cy - 50 - 10 * tileHeight / axis + east * tileHeight / 2};
}
for (const [east, shadowEast] of [[1, 2], [3, 6]]) {
  const c = cloud(east);
  near(2 * c.planeX - sun.x, 160 + shadowEast * 14);
  near(2 * c.planeY - sun.y, cy + shadowEast * tileHeight / 2);
  rectangles.length = 0;
  world.drawCloudShadowsOnIsland([c], 60, 0, 5, 0);
  assert.equal(rectangles.length, 25);
  near(rectangles[0][0], 160 + shadowEast * 14 - 5 * 0.78 / 2);
}
rectangles.length = 0;
world.drawProjectedCloudShadows([cloud(3)], 40, 0, 5, 0, false);
assert.equal(rectangles.length, 25, 'the K5 shadow is drawn outside the island');
near(rectangles[0][0], 244 - 5 * 0.78 / 2);
world.drawMoon({altitude: 60, hourAngleDegrees: 0, illumination: 1, waxing: true}, 0, -20);
near(window.testMoonCentre.x, Math.round(sun.x * 2) / 2);
near(window.testMoonCentre.y, Math.round(sun.y * 2) / 2);
for (let angle = -180; angle <= 180; angle++) {
  assert(world.sunPlanePosition(angle).y >= sun.y - 1e-9, 'E5 must be the arc peak');
}
for (const [windFrom, expectedX, expectedY] of [[90,-1,-1],[270,1,1],[180,1,-1],[0,-1,1]]) {
  world.reset();
  const weather = {cloudCover: 5, windSpeed: 10, windDirection: windFrom};
  const a = world.createCloudStates(0, weather)[0];
  const b = world.createCloudStates(1, weather)[0];
  assert.equal(Math.sign(b.planeX - a.planeX), expectedX);
  assert.equal(Math.sign(b.planeY - a.planeY), expectedY);
  const c = world.createCloudStates(2, {...weather, windSpeed: 0})[0];
  near(c.planeX, b.planeX); near(c.planeY, b.planeY);
}
for (const [cover, count] of [[0,1],[32,2],[58,3],[82,4]]) {
  world.reset(); assert.equal(world.createCloudStates(0, {cloudCover: cover, windSpeed: 0}).length, count);
}
console.log('Base World: E5/F5/G5, H5/K5, Moon arc, outside shadow, cardinal wind, calm and cloud counts OK');

// Surface object shadows are projected away from the shared light on the island plane.
const anchor = {x: 160, y: cy}, width = 12, depth = 5, height = 25;
for (const angle of [-90, 0, 90]) {
  const light = world.sunPlanePosition(angle);
  const points = world.objectShadowPolygon(anchor, width, depth, height, light);
  const ratio = height / (100 - height);
  const dx = (anchor.x - light.x) * ratio;
  const dy = (anchor.y - light.y - 100) * ratio;
  const xs = points.map(p => p[0]), ys = points.map(p => p[1]);
  const rawW = width + Math.abs(dx), rawH = depth + Math.abs(dy);
  const scale = Math.min(0.5, width * 0.5 / rawW, height * 0.5 / rawH);
  near(Math.min(...xs), anchor.x + scale * Math.min(-width / 2, -width / 2 + dx));
  near(Math.max(...xs), anchor.x + scale * Math.max(width / 2, width / 2 + dx));
  near(Math.min(...ys), anchor.y + scale * Math.min(-depth / 2, -depth / 2 + dy));
  near(Math.max(...ys), anchor.y + scale * Math.max(depth / 2, depth / 2 + dy));
  assert(Math.max(...xs) - Math.min(...xs) <= width / 2 + 1e-6);
  assert(Math.max(...ys) - Math.min(...ys) <= height / 2 + 1e-6);
}
console.log('Surface object shadows: shared light projection at three angles OK');
