const assert=require('node:assert/strict'),pet=require('../preview/sparrow-pet').create(),a={x:100,y:100};
let p=pet.update(0,'world',a,1,0,false,false,false,false);assert.equal(p.x,93);assert.equal(p.y,88.5);assert.equal(p.hovering,true);assert.equal(p.flap,true);
p=pet.update(200,'world',a,1,0,false,false,false,false);assert.ok(p.hovering && p.y<95 && p.x>93,'idle flight circles above ground');
p=pet.update(300,'world',{x:110,y:100},1,0,false,false,true,false);assert.ok(p.x>93&&p.x<103,'smoothly follows');
p=pet.update(400,'other',a,1,1,false,false,false,false);assert.equal(p.x,100.5);assert.ok(p.y<80,'perches on cap');assert.equal(p.hovering,false);
p=pet.update(500,'other',a,1,1,true,true,false,true);assert.equal(p.x,99.5);assert.ok(p.y<80,'reduced motion places it without animation');
pet.reset();pet.update(1000,'world',a,1,0,false,false,false,false);p=pet.update(1120,'world',a,1,0,false,false,true,false);assert.equal(p.flap,false);p=pet.update(1280,'world',a,1,0,false,false,true,false);assert.equal(p.flap,true);p=pet.update(1440,'world',a,1,0,false,false,true,true);assert.equal(p.flap,false,'reduced motion disables wing animation');
pet.reset();p=pet.update(400,'other',a,1,0,false,false,false,false);assert.ok(p.y<100,'idle bird flies');
console.log('Sparrow follow, perch, world reset and reduced motion: OK');
