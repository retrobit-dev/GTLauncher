const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview'),dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://test.invalid/',runScripts:'outside-only'}),w=dom.window,d=w.document;
for(const dialog of d.querySelectorAll('dialog'))dialog.showModal=()=>dialog.open=true;
let current=[],outlines=[];
const ctx=new Proxy({createLinearGradient:()=>({addColorStop(){}}),createRadialGradient:()=>({addColorStop(){}}),beginPath(){current=[]},moveTo(x,y){current.push([x,y])},lineTo(x,y){current.push([x,y])},stroke(){if(this.strokeStyle==='#fff'&&this.lineWidth===1.2)outlines.push(current.slice())}},{get:(o,k)=>k in o?o[k]:()=>{},set:(o,k,v)=>(o[k]=v,true)});
w.HTMLCanvasElement.prototype.getContext=()=>ctx;w.matchMedia=()=>({matches:true});w.requestAnimationFrame=()=>0;w.fetch=async()=>({ok:false});
for(const name of ['worlds.js','app.js'])w.eval(fs.readFileSync(path.join(root,name),'utf8'));
d.getElementById('world-new').click();d.getElementById('world-name').value='Marker';d.getElementById('world-character').checked=true;d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
const hoe=d.querySelector('[data-dock="hoe"]'),hand=d.querySelector('[data-dock="hand"]'),world=w.worldManager.getActive();hand.setAttribute('aria-pressed','false');hoe.setAttribute('aria-pressed','true');
for(const [facing,dx,dy] of [['N',0,-1],['E',1,0],['S',0,1],['W',-1,0]]) {
 const character={...w.worldManager.getCharacter(),column:12,row:12,facing};assert(w.worldManager.updateCharacter(world.id,character));outlines=[];w.dispatchEvent(new w.Event('gameplaytoolchange'));
 assert.equal(outlines.length,1,`${facing}: one outline`);const points=outlines[0],centre=w.sceneEditing.centre({column:12+dx,row:12+dy});assert.equal(points.length,4);assert(Math.abs((points[1][0]+points[3][0])/2-centre.x)<1e-6);assert(Math.abs((points[0][1]+points[2][1])/2-centre.y)<1e-6);
}
assert(w.worldManager.updateCharacter(world.id,{...w.worldManager.getCharacter(),column:0,row:12,facing:'W'}));outlines=[];w.dispatchEvent(new w.Event('gameplaytoolchange'));assert.equal(outlines.length,0,'no marker outside world');
assert(w.worldManager.updateCharacter(world.id,{...w.worldManager.getCharacter(),column:12,row:12,facing:'E'}));const tiles=w.worldManager.getActive().tiles.slice();tiles[12*world.gridSize+13]=0;assert(w.worldManager.updateTiles(world.id,tiles));outlines=[];w.dispatchEvent(new w.Event('gameplaytoolchange'));assert.equal(outlines.length,0,'no marker on missing tile');tiles[12*world.gridSize+13]=1;assert(w.worldManager.updateTiles(world.id,tiles));
hoe.setAttribute('aria-pressed','false');outlines=[];w.dispatchEvent(new w.Event('gameplaytoolchange'));assert.equal(outlines.length,0,'no marker when unselected');
hand.setAttribute('aria-pressed','true');outlines=[];w.dispatchEvent(new w.Event('gameplaytoolchange'));assert.equal(outlines.length,1,'hand also shows marker');
dom.window.close();console.log('Hoe marker tracks all four facing directions, boundaries and selection: OK');
