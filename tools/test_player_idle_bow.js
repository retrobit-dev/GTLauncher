const assert=require('node:assert/strict'),bow=require('../preview/player-idle-bow');
assert.equal(bow.amount(2999),0);assert.equal(bow.amount(3000),0);assert.equal(bow.amount(3250),.5);assert.equal(bow.amount(3500),1);assert.equal(bow.amount(4750),.5);assert.equal(bow.amount(5000),0);assert.equal(bow.amount(6000),0);assert.equal(bow.amount(6500),1);
const c=bow.create();assert.equal(c.update(100,'world:2:3',true),0);assert.equal(c.update(3600,'world:2:3',true),1);assert.equal(c.update(3601,null,false),0);assert.equal(c.update(4000,'world:2:3',true),0);assert.equal(c.update(7500,'world:3:3',true),0);assert.equal(c.update(11000,'other:3:3',true),0);
assert.equal(bow.frame(0),0);assert.equal(bow.frame(.19),0);assert.equal(bow.frame(.2),1);assert.equal(bow.frame(.74),1);assert.equal(bow.frame(.75),2);assert.equal(bow.frame(1),2);
const sit=bow.create();sit.update(0,'tile',true);assert.equal(sit.update(15000,'tile',true),0);assert.equal(sit.sitting(),0);sit.update(15900,'tile',true);assert.equal(sit.sitting(),.5);sit.update(16800,'tile',true);assert.equal(sit.sitting(),1);sit.update(16801,null,false);assert.equal(sit.sitting(),0);
const water=bow.create();water.update(0,'water',true,false);assert.equal(water.update(15500,'water',true,false),1);
console.log('Idle timing, movement resets and three whole-frame poses: OK');
