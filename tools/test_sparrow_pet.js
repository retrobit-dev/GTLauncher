const assert=require('node:assert/strict'),pet=require('../preview/sparrow-pet').create(),a={x:100,y:100};
let p=pet.update(0,'world',a,1,0,false,false,false,false);assert.equal(p.x,93);assert.equal(p.y,100.5);
p=pet.update(100,'world',{x:110,y:100},1,0,false,false,true,false);assert.ok(p.x>93&&p.x<103,'smoothly follows');
p=pet.update(200,'other',a,1,1,false,false,false,false);assert.equal(p.x,100.5);assert.ok(p.y<80,'perches on cap');
p=pet.update(300,'other',a,1,1,true,true,false,true);assert.equal(p.x,99.5);assert.ok(p.y<80,'reduced motion places it without animation');
pet.reset();p=pet.update(400,'other',a,1,0,false,false,false,false);assert.equal(p.x,93);
console.log('Sparrow follow, perch, world reset and reduced motion: OK');
