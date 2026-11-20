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
// Production deck matches the stored footprint exactly, regardless of length.
for (const length of [3,4,5]) for (const width of [1,2,3]) {
  const [a,u,v,w]=b.wideDeck(length,width,28,12);
  assert.equal(w.x-a.x,width*14);assert.equal(w.y-a.y,width*6);
  assert.equal(u.x-a.x,length*14);assert.equal(u.y-a.y,-length*6);
}
console.log('Bridge deck widths match their tile footprints: OK');

// Ramps begin at the outer end, rise over one tile, and remain level in the middle.
for(const length of [3,4,5])for(const axis of ['E','S']){
  const bridge={column:4,row:5,axis,length,width:1};
  const at=t=>({column:4+(axis==='E'?t:0),row:5+(axis==='S'?t:0)});
  const h=p=>b.riseAt([bridge],p.column,p.row,8);
  assert.strictEqual(h(at(-.5)),0);
  assert.strictEqual(h(at(0)),2);
  assert.strictEqual(h(at(.5)),4);
  assert(Math.abs(h(at(length-1))-2)<1e-9);
  assert.strictEqual(h(at(length-.5)),0);
  assert.strictEqual(h({column:0,row:0}),0);
  const corners=b.wideDeck(length,1,16,8,.1),sections=b.profilePoints(corners,length,4);
  assert.strictEqual(sections[0][0].y,corners[0].y);
  assert.strictEqual(sections[3][0].y,corners[1].y);
  assert(b.wideContains({x:(sections[1][0].x+sections[1][1].x)/2,y:(sections[1][0].y+sections[1][1].y)/2},corners,8,length));
}
console.log('Sloped bridge ends, continuous player rise, and picking: OK');
for(const axis of ['E','S']) for(const length of [3,4,5]) {
 const world={gridSize:20,tiles:Array(400).fill(1)};
 const bridge={column:5,row:5,axis,material:'wood',gap:true,free:true,deck:1,centered:true,...(length===3?{}:{length})};
 assert(b.valid(world,[bridge]));
 const neighbor=axis==='E'?{column:6,row:4}:{column:4,row:6};
 assert(!b.valid({...world,orangeTrees:[{...neighbor,size:1}]},[bridge]));
 assert(!b.valid(world,[bridge,{...bridge,...neighbor,centered:true}]));
 assert(!b.valid(world,[{...bridge,...(axis==='E'?{row:0}:{column:0})}]));
 assert.equal(b.riseAt([bridge],neighbor.column,neighbor.row,8),0);
 assert(b.permitsStep([bridge],{column:5,row:5},axis==='E'?{column:6,row:5}:{column:5,row:6}));
 const legacy={...bridge};delete legacy.centered;assert(b.valid(world,[legacy]));
 for(let q=0;q<4;q++){const rotated=require('../preview/world-view-rotation').bridge(bridge,20,q);assert(rotated);assert(b.valid(world,[{...bridge,column:rotated.column,row:rotated.row,axis:rotated.axis}]));}
}
console.log('Centered two-tile deck reserves side cells, retains center route and rotates: OK');
// Inset posts and rails fit inside the 1-tile deck in projected ground space,
// even at the editor's flattest tilt. Check width and both end margins.
for(const length of [3,4,5])for(const elevation of [5,25,53]){
 const w=28,h=w*Math.sin(elevation*Math.PI/180),corners=b.wideDeck(length-1,1,w,h),rails=b.insetRails(corners,length,2,true);
 const [a,u,,v]=corners;
 const uv=p=>{const x=p.x-a.x,y=p.y-a.y;return {along:(x/(w/2)-y/(h/2))/(2*(length-1)),across:(x/(w/2)+y/(h/2))/2};};
 // Offset each post/rail back down to its ramp ground elevation before checking.
 for(let i=0;i<4;i++)for(let side=0;side<2;side++){
   const t=[.22/(length-1),.5/(length-1),1-.5/(length-1),1-.22/(length-1)][i],p={...rails[i][side]};p.y+=b.rise(t,length,2,true);
   for(const dx of [-.825,.825])for(const dy of [-Math.min(.35,h*.035)-.175,Math.min(.35,h*.035)+.175]){
     const q=uv({x:p.x+dx,y:p.y+dy});assert(q.across>=0&&q.across<=1);assert(q.along>=0&&q.along<=1);
   }
 }
 const post=rails[0][0];assert(b.wideContains({x:post.x,y:post.y-2},corners,4,length,.65,true));
}
for(const axis of ['E','S']) {
 const world={gridSize:20,tiles:Array(400).fill(1)},bridge={column:0,row:0,axis,material:'wood',gap:true,free:true,deck:1,inset:true};
 assert(b.valid(world,[bridge]));assert(!b.valid(world,[{...bridge,centered:true}]));
}
console.log('One-tile inset posts and rail footprint, picking and edge placement: OK');

for(const length of [3,4,5])for(const axis of ['E','S']) {
 const bridge={column:6,row:6,axis,material:'wood',gap:true,free:true,deck:1,inset:true,...(length===3?{}:{length})};
 const h=t=>b.riseAt([bridge],6+(axis==='E'?t:0),6+(axis==='S'?t:0),4);
 for(const t of [-.5,0,length-1,length-.5])assert.equal(h(t),0);
 for(const t of [.5,length-1.5])assert(Math.abs(h(t)-2)<1e-8);
 assert.equal(h(.25),1);assert(Math.abs(h(length-1.25)-1)<1e-8);
 const [a,c,,d]=b.wideDeck(length-1,1,28,12);
 assert.equal(c.x-a.x,(length-1)*14);assert.equal(c.y-a.y,-(length-1)*6);
}
console.log('New bridge ramp ends at both endpoint block centers: OK');
// Flat deck spans the 3/4/5 tile footprint plus a quarter tile on both ends.
for(const length of [3,4,5])for(const axis of ['E','S'])for(const material of ['wood','stone']) {
 const bridge={column:5,row:5,axis,material,gap:true,free:true,deck:1,straight:true,...(length===3?{}:{length})};
 const world={gridSize:20,tiles:Array(400).fill(1)};
 assert(b.valid(world,[bridge]));
 const corners=b.wideDeck(length+.5,1,28,12),[a,u,,d]=corners;
 assert.equal(u.x-a.x,(length+.5)*14);assert.equal(u.y-a.y,-(length+.5)*6);assert.equal(d.x-a.x,14);assert.equal(d.y-a.y,6);
 const rails=b.insetRails(corners,length+.5,0);assert.equal(rails.length,4);
 for(let side=0;side<2;side++)for(let i=0;i<4;i++){
  const p=rails[i][side],normal=(p.x-a.x)/14+(p.y-a.y)/6;
  assert(Math.abs(normal/2-(side?.78:.22))<1e-9);
 }
 const post=rails[0][0];assert(b.wideContains({x:post.x,y:post.y-2},corners,4,length,.65,false,true));
 for(const t of [0,.5,1,length-1])assert.equal(b.riseAt([bridge],5+(axis==='E'?t:0),5+(axis==='S'?t:0),4),0);
 const endpoint=axis==='E'?{column:4,row:5}:{column:5,row:4};
 assert(!b.valid({...world,orangeTrees:[{...endpoint,size:1}]},[bridge]));
 assert(!b.valid(world,[{...bridge,...(axis==='E'?{column:0}:{row:0})}]));
 for(let q=0;q<4;q++) {const rotated=require('../preview/world-view-rotation').bridge(bridge,20,q);assert(b.valid(world,[{...bridge,column:rotated.column,row:rotated.row,axis:rotated.axis}]));}
}
console.log('Flat bridge extends exactly quarter tile beyond each end, with inset straight rails: OK');
