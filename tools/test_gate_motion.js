const assert=require('assert');const gate=require('../preview/gate-motion.js');
for(const [edge,inside,outside] of [['N',[0,0],[0,-1]],['S',[0,0],[0,1]],['E',[0,0],[1,0]],['W',[0,0],[-1,0]]]) {
 const g={column:0,row:0,edge};for(const [column,row] of [inside,outside])assert.equal(gate.openness(g,{column,row}),1);
 assert.equal(gate.openness(g,{column:10,row:10}),0);
}
assert.equal(gate.openness({column:0,row:0,edge:'E'},{column:1.25,row:0}),.5);
assert.equal(gate.openness({column:0,row:0,edge:'bad'},{column:0,row:0}),0);
assert.equal(gate.openness(null,{column:0,row:0}),0);
for(const [edge,first,second] of [['N',[0,0],[0,-1]],['S',[0,0],[0,1]],['E',[0,0],[1,0]],['W',[0,0],[-1,0]]]) {
 const g={column:0,row:0,edge},a=gate.update(g,{column:first[0],row:first[1]});
 const b=gate.update(g,{column:second[0],row:second[1]},a.side);
 assert.equal(a.amount,1);assert.equal(b.side,a.side,'do not reverse while crossing');
 assert.equal(gate.update(g,{column:10,row:10},b.side).side,0);
 assert.equal(gate.update(g,{column:second[0],row:second[1]}).side,-a.side,'approach from opposite side');
}
console.log('Automatic gate opening geometry: OK');
