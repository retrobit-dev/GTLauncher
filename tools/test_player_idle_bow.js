const assert=require('node:assert/strict'),bow=require('../preview/player-idle-bow');
assert.equal(bow.amount(2999),0);assert.equal(bow.amount(3000),0);assert.equal(bow.amount(3250),.5);assert.equal(bow.amount(3500),1);assert.equal(bow.amount(4750),.5);assert.equal(bow.amount(5000),0);assert.equal(bow.amount(6000),0);assert.equal(bow.amount(6500),1);
const c=bow.create();assert.equal(c.update(100,'world:2:3',true),0);assert.equal(c.update(3600,'world:2:3',true),1);assert.equal(c.update(3601,null,false),0);assert.equal(c.update(4000,'world:2:3',true),0);assert.equal(c.update(7500,'world:3:3',true),0);assert.equal(c.update(11000,'other:3:3',true),0);
const calls=[],ctx={clearRect(...p){calls.push(['clear',...p])},drawImage(...p){calls.push(['draw',...p.slice(1)])},save(){},restore(){},transform(...p){calls.push(['transform',...p])}};
bow.draw(ctx,{},432,800,1);const bottom=calls.find(c=>c[0]==='draw');assert.deepEqual(bottom.slice(1),[0,544,432,256,0,544,432,256]);
const transform=calls.find(c=>c[0]==='transform');assert.equal(transform[4],.8);assert.ok(transform[3]<0);assert.ok(Math.abs(transform[6]-544*.2)<1e-9);
console.log('Idle timing, reset, smooth bow and fixed lower body: OK');
