const assert=require('node:assert/strict'),b=require('../preview/bridge-geometry'),p=require('../preview/gameplay-path');
const size=20,tiles=Array(size*size).fill(1),w={gridSize:size,tiles},bridge={column:5,row:5,axis:'E',material:'wood',gap:true};
assert.equal(b.valid(w,[bridge]),false,'flat ground is not a hole');tiles[5*size+6]=0;
assert(b.valid(w,[bridge]),'one lower empty tile');w.bridges=[bridge];assert.deepEqual(p.find(w,{column:5,row:5},{column:7,row:5},null),[{column:5,row:5},{column:6,row:5},{column:7,row:5}]);assert.equal(b.deckHeight(w,{column:6,row:5}),1);
assert(!b.permitsStep([bridge],{column:6,row:4},{column:6,row:5}),'no side entry');
const rotated={column:5,row:5,axis:'S',material:'stone',length:4,gap:true};tiles[6*size+5]=tiles[7*size+5]=0;assert(b.valid(w,[rotated]));tiles[8*size+5]=3;assert(!b.valid(w,[rotated]),'dry endpoints');
const stacks=tiles.map(t=>t?[t]:[]);tiles[5*size+6]=1;stacks[5*size+5]=stacks[5*size+7]=[2,1];stacks[5*size+6]=[1];assert(b.valid({...w,blockStacks:stacks},[bridge]),'one-block depression');stacks[5*size+6]=[];tiles[5*size+6]=0;assert(!b.valid({...w,blockStacks:stacks},[bridge]),'two blocks deep rejected');
console.log('Gap bridge validation, traversal and one-level supporting surfaces: OK');
{
 const tiles=Array(400).fill(1),w={gridSize:20,tiles},bridge={column:5,row:5,axis:'E',material:'wood',gap:true,overWater:true};tiles[106]=3;
 assert(b.valid(w,[bridge]),'same-level water accepted');assert(!b.valid(w,[{...bridge,overWater:undefined}]),'v23 cannot silently accept same-level water');
 tiles[106]=1;assert(!b.valid(w,[bridge]),'dry middle not accepted');tiles[105]=3;tiles[106]=3;assert(!b.valid(w,[bridge]),'endpoint must be land');
}
console.log('Same-level water supported with dry endpoints; dry span rejected: OK');
for(const length of [3,4,5])for(const axis of ['E','S']) {
 const tiles=Array(400).fill(1),w={gridSize:20,tiles},bridge={column:5,row:5,axis,material:'wood',gap:true,overWater:true,...(length===3?{}:{length})};
 for(let i=1;i<length-1;i++){const c=5+(axis==='E'?i:0),r=5+(axis==='S'?i:0);tiles[r*20+c]=3;}
 assert(b.valid(w,[bridge]),`${length} ${axis} water span`);w.bridges=[bridge];const target={column:5+(axis==='E'?length-1:0),row:5+(axis==='S'?length-1:0)};assert.equal(p.find(w,{column:5,row:5},target,null).length,length);
}
console.log('3/4/5-tile water bridges support both axes and route end-to-end: OK');
