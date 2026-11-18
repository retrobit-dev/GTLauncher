const assert=require('node:assert/strict'),c=require('../preview/cloud-layout');
assert.equal(c.count(0,0),3); assert.equal(c.count(100,0),4);
assert.equal(c.count(20,3),5);assert.equal(c.count(100,63),8);
assert.equal(c.cellSize(28,14,28,14),4);
assert.equal(c.fade(0,true),1);assert.ok(Math.abs(c.fade(12,true)-.6)<1e-10);assert.equal(c.fade(12,false),1);
assert.equal(c.wet(20,63),true); assert.equal(c.wet(5,0),false);
console.log('Cloud count, 4-tile sizing, soft weather shadow and reduced-motion fade: OK');
