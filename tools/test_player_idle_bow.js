const assert=require('node:assert/strict'),bow=require('../preview/player-idle-bow');
assert.equal(bow.amount(19999),0);assert.equal(bow.amount(20000),0);assert.equal(bow.amount(21000),1);assert.equal(bow.amount(22500),1);assert.equal(bow.amount(24000),0);assert.equal(bow.amount(44000),0);assert.equal(bow.amount(45000),1);
const c=bow.create();assert.equal(c.update(100,'world:2:3',true),0);assert.equal(c.update(21100,'world:2:3',true),1);assert.equal(c.update(21101,null,false),0);assert.equal(c.update(22000,'world:2:3',true),0);assert.equal(c.update(43000,'world:3:3',true),0);assert.equal(c.update(64000,'other:3:3',true),0);
const calls=[],ctx={clearRect(...p){calls.push(['clear',...p])},drawImage(...p){calls.push(['draw',...p.slice(1)])},save(){},restore(){},transform(...p){calls.push(['transform',...p])}};
bow.draw(ctx,{},432,800,1);const bottom=calls.find(c=>c[0]==='draw');assert.deepEqual(bottom.slice(1),[0,544,432,256,0,544,432,256]);
const transform=calls.find(c=>c[0]==='transform');assert.equal(transform[4],.8);assert.ok(transform[3]<0);assert.ok(Math.abs(transform[6]-544*.2)<1e-9);
console.log('Idle timing, reset, smooth bow and fixed lower body: OK');
