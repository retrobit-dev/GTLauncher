const assert=require('node:assert/strict');
const B=require('../preview/bridge-geometry.js');
const R=require('../preview/world-view-rotation.js');
const n=20, stacks=Array.from({length:n*n},()=>[1]);
stacks[5*n+5]=[]; stacks[5*n+6]=[1,1,3]; stacks[5*n+7]=[1,1];
const world={gridSize:n,tiles:stacks.map(s=>s.at(-1)||0),blockStacks:stacks,ramps:[{column:6,row:5,direction:'E',material:3}]};
const bridge={column:5,row:5,axis:'E',material:'wood',gap:true,free:true,deck:1};
assert(B.valid(world,[bridge]));
for(const p of B.cells(bridge))assert.equal(B.deckHeight({...world,bridges:[bridge]},p),1);
assert(!B.valid(world,[{...bridge,deck:0}]));assert(!B.valid(world,[{...bridge,free:false}]));
assert(!B.valid(world,[{...bridge,column:19}]));
const withBridge={...world,bridges:[bridge]};
for(let q=0;q<4;q++){const w=R.projectWorld(withBridge,q);const b=w.bridges[0];assert.equal(b.deck,1);assert(B.valid(w,[b]));assert.equal(B.deckHeight(w,{column:b.column,row:b.row}),1);}
assert(!B.valid({...world,house:{column:5,row:5}},[bridge]));
console.log('Free bridges accept dry, water, gaps, ramps and differing heights; deck survives rotation: OK');
