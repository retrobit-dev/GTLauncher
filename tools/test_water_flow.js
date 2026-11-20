const assert=require('node:assert/strict'),flow=require('../preview/water-flow');
const size=20,tiles=Array(size*size).fill(0),stacks=tiles.map(()=>[]),set=(c,r,stack)=>{tiles[r*size+c]=stack.at(-1)||0;stacks[r*size+c]=stack};
const w={gridSize:size,tiles,blockStacks:stacks};set(5,5,[3]);set(6,5,[3]);set(7,5,[3]);assert.equal(flow.direction({...w},6,5),'E');set(5,5,[]);set(7,5,[]);set(6,4,[3]);set(6,6,[3]);assert.equal(flow.direction({...w},6,5),'S');set(6,5,[1,3]);assert.equal(flow.direction({...w},6,5),'N');set(6,4,[1,3]);assert.equal(flow.direction({...w},6,5),'S');assert.equal(flow.direction({...w},3,3),null);
console.log('Water current follows long axis and prioritizes downhill neighbours: OK');
