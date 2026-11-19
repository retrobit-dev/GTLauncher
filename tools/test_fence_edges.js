const assert=require('node:assert/strict');
const {key,blocks}=require('../preview/fence-edges.js');
const east={column:4,row:5,edge:'E'},west={column:5,row:5,edge:'W'};
assert.equal(key(east),key(west));
assert.equal(key({column:4,row:5,edge:'S'}),key({column:4,row:6,edge:'N'}));
for(const fence of [east,west]) {
 assert(blocks(fence,{column:4,row:5},{column:5,row:5}));
 assert(blocks(fence,{column:5,row:5},{column:4,row:5}));
 assert(!blocks(fence,{column:4,row:5},{column:4,row:6}));
 assert(!blocks(fence,{column:4,row:5},{column:5,row:6}));
}
assert.equal(key({column:4,row:5,edge:'invalid'}),null);
console.log('Canonical fence edge and bidirectional movement checks: OK');
