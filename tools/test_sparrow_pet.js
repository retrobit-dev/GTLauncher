const assert=require('node:assert/strict'),pet=require('../preview/sparrow-pet').create(),a={x:100,y:100};
let p=pet.update(0,'world',a,1,0,false,false,false,false,75);assert.equal(p.x,100);assert.equal(p.y,71.5);assert.equal(p.hovering,true);assert.equal(p.flap,true);
p=pet.update(300,'world',a,1,0,false,false,false,false,75);assert.ok(p.x>100&&p.y<75,'idle flight stays above head');assert.equal(p.flap,false);
p=pet.update(600,'world',{x:110,y:100},1,0,false,false,true,false,75);assert.ok(Math.abs(p.x-110)<1.6&&p.y<75,'follows above a moving player');assert.equal(p.flap,true);
p=pet.update(900,'world',a,1,1,false,false,false,false,77);assert.equal(p.x,100.5);assert.equal(p.y,77);assert.equal(p.hovering,false);assert.equal(p.flap,false,'perches while seated');
p=pet.update(1200,'world',a,1,0,true,true,false,true,75);assert.equal(p.x,100);assert.equal(p.y,71.5);assert.equal(p.flap,false,'reduced motion disables wing animation');
console.log('Sparrow hover above player, facing, perch and reduced motion: OK');
