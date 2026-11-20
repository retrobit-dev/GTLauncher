const assert=require('node:assert/strict'),b=require('../preview/bridge-geometry.js'),path=require('../preview/gameplay-path.js');
for(const axis of ['E','S'])for(const material of ['wood','stone']) {
 const bridge={column:5,row:5,axis,material},c=b.cells(bridge),p=(x,y)=>({column:x,row:y});assert.equal(c.length,3);
 assert(b.permitsStep([bridge],c[0],c[1]));assert(b.permitsStep([bridge],c[1],c[2]));
 const start=axis==='E'?p(4,5):p(5,4),end=axis==='E'?p(8,5):p(5,8),side=axis==='E'?p(6,4):p(4,6);
 assert(b.permitsStep([bridge],start,c[0]));assert(b.permitsStep([bridge],c[2],end));assert(!b.permitsStep([bridge],c[1],side));assert(!b.permitsStep([bridge],side,c[1]));
}
assert.equal(b.cells({column:1,row:1,axis:'W',material:'wood'}),null);
const w={gridSize:20,tiles:Array(400).fill(1),blockStacks:Array.from({length:400},()=>[1])};w.blockStacks[0]=[7,8];w.tiles[0]=8;
assert(path.find(w,{column:3,row:3},{column:4,row:3},null),'new grass-topped blocks must not disable stacked-world pathfinding');
console.log('Bridge footprint and railing rules; v14 stacked pathfinding: OK');
{
 const world={gridSize:20,tiles:Array(400).fill(1)}, bridge={column:5,row:5,axis:'E',material:'wood'};
 world.tiles[5*20+6]=3;assert(b.valid(world,[bridge]),'dry ends and water in the middle');
 assert(!b.valid(world,[bridge,bridge]),'overlapping bridges');
 world.tiles[5*20+5]=3;assert(!b.valid(world,[bridge]),'water at entrance');world.tiles[5*20+5]=1;
 world.orangeTrees=[{column:6,row:5,kind:'wild-grass',size:1}];assert(!b.valid(world,[bridge]));delete world.orangeTrees;
 world.fences=[{column:6,row:4,edge:'S',material:'wood'}];assert(!b.valid(world,[bridge]),'canonical adjacent fence conflicts');delete world.fences;
 world.blockStacks=world.tiles.map(t=>[t]);world.blockStacks[5*20+6]=[1,3];assert(!b.valid(world,[bridge]),'uneven ground');
}
console.log('Bridge placement rejects overlap, water entrances, obstructions and uneven terrain: OK');
for (const length of [4,5]) for (const axis of ['E','S']) {
  const bridge={column:3,row:3,axis,material:'wood',length}, cells=b.cells(bridge);
  assert.equal(cells.length,length);
  const tiles=Array(100).fill(1); for(const p of cells.slice(1,-1))tiles[p.row*10+p.column]=3;
  const w={gridSize:10,tiles};assert.equal(b.valid(w,[bridge]),true);
  assert.equal(b.permitsStep([bridge],cells[0],cells[1]),true);
  const last=cells.at(-1),out={column:last.column+(axis==='E'?1:0),row:last.row+(axis==='S'?1:0)};
  assert.equal(b.permitsStep([bridge],last,out),true);
  const side={column:cells[1].column+(axis==='S'?1:0),row:cells[1].row+(axis==='E'?1:0)};
  assert.equal(b.permitsStep([bridge],cells[1],side),false);
  tiles[last.row*10+last.column]=3;assert.equal(b.valid(w,[bridge]),false);
}
console.log('4×1 and 5×1 bridge footprints, water and end-only traversal: OK');
// The deck's calibrated edges must coincide with an N-by-1 tile parallelogram.
for(const [material,points] of Object.entries({wood:[[0,284],[430,47],[131,359]],stone:[[35,253],[450,50],[158,321]]}))for(const length of [3,4,5])for(const [w,h] of [[28,14],[28,10]]) {
 const m=b.spriteTransform(material,length,w,h), project=([x,y])=>({x:m.a*x+m.c*y+m.e,y:m.b*x+m.d*y+m.f}),[p,u,v]=points.map(project);
 const near=(a,z)=>assert.ok(Math.abs(a-z)<1e-8,`${material} ${length}: ${a} != ${z}`);
 near(u.x-p.x,length*w/2);near(u.y-p.y,-length*h/2);near(v.x-p.x,w/2);near(v.y-p.y,h/2);
 // Mirroring for the E axis reverses x but preserves the same tile edge slopes.
 near(-(u.x-p.x),-length*w/2);near(-(v.x-p.x),-w/2);
}
console.log('Calibrated bridge deck edges match 3/4/5 by 1 tile footprint: OK');
for(const axis of ['E','S'])for(const length of [3,4,5]){
 const width=length===5?3:2,bridge={column:5,row:5,axis,material:'wood',...(length===3?{}:{length}),width},cells=b.cells(bridge);
 assert.equal(cells.length,length*width);const w={gridSize:20,tiles:Array(400).fill(1)};assert(b.valid(w,[bridge]));
 const cell=(i,j)=>({column:5+(axis==='E'?i:j),row:5+(axis==='S'?i:j)});
 for(let j=0;j<width;j++){assert(b.permitsStep([bridge],cell(0,j),cell(1,j)));if(j+1<width)assert(b.permitsStep([bridge],cell(1,j),cell(1,j+1)));w.tiles[cell(0,j).row*20+cell(0,j).column]=3;assert(!b.valid(w,[bridge]));w.tiles[cell(0,j).row*20+cell(0,j).column]=1;}
 assert(!b.permitsStep([bridge],cell(1,0),axis==='E'?{column:6,row:4}:{column:4,row:6}));
 for(let q=0;q<4;q++){const r=require('../preview/world-view-rotation').bridge(bridge,20,q);assert(r);const actual=new Set(b.cells({...bridge,...r}).map(p=>`${p.column},${p.row}`));for(const p of cells){const t=require('../preview/world-view-rotation').point(p.column,p.row,20,q);assert(actual.has(`${t.column},${t.row}`));}}
}
console.log('Wide bridges validate every tile, allow lateral deck movement, block side entry, and rotate all cells: OK');
for(const length of [3,4,5]) {
 const width=length===5?3:2,poly=b.wideDeck(length,width,28,12),[a,u,v,w]=poly;
 assert.equal(u.x-a.x,length*14);assert.equal(u.y-a.y,-length*6);assert.equal(w.x-a.x,width*14);assert.equal(w.y-a.y,width*6);
 for(const p of poly){assert(b.wideContains({x:p.x,y:p.y-4},poly),'vertical post hit above corner');}
}
console.log('Wide decks align to tile edges and corner posts stay vertical: OK');
