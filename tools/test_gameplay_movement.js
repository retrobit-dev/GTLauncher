const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const Path=require('../preview/gameplay-path.js'),root=path.join(__dirname,'../preview');
const tiles=Array(400).fill(1),w20={gridSize:20,tiles};tiles[1]=3;tiles[20]=0;
assert.equal(Path.find(w20,{column:0,row:0},{column:2,row:0},null),null,'blocked start has no route');
tiles[20]=1;const route=Path.find(w20,{column:0,row:0},{column:2,row:0},null);assert(route.length>3 && !route.some(p=>p.column===1&&p.row===0));
assert.equal(Path.find(w20,{column:0,row:0},{column:1,row:0},null),null,'water cannot be destination');
assert.equal(Path.find(w20,{column:0,row:0},{column:20,row:0},null),null);
assert.equal(Path.find(w20,{column:0,row:0},{column:4,row:4},{column:3,row:3,visible:true}),null,'house blocks footprint');
assert(Path.find({}, {column:0,row:0},{column:8,row:8},null));assert.equal(Path.find({}, {column:0,row:0},{column:9,row:8},null),null);
const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://example.test/',runScripts:'outside-only'}),win=dom.window,d=win.document;
for(const dialog of d.querySelectorAll('dialog')){dialog.showModal=()=>dialog.open=true;dialog.close=()=>dialog.open=false;}
const ctx=new Proxy({createLinearGradient:()=>({addColorStop(){}}),createRadialGradient:()=>({addColorStop(){}})},{get:(o,k)=>k in o?o[k]:()=>{},set:(o,k,v)=>(o[k]=v,true)});
win.HTMLCanvasElement.prototype.getContext=()=>ctx;win.matchMedia=()=>({matches:true});win.requestAnimationFrame=()=>0;win.fetch=async()=>({ok:false});
const canvas=d.getElementById('sky-canvas');canvas.getBoundingClientRect=()=>({left:0,top:0,width:640,height:400});
for(const name of ['worlds.js','object-editor.js','app.js','preview-modes.js','editor-camera.js','gameplay-path.js','gameplay-movement.js'])win.eval(fs.readFileSync(path.join(root,name),'utf8'));
d.getElementById('world-new').click();d.getElementById('world-name').value='Bergerak';d.getElementById('world-character').checked=true;d.getElementById('world-form').dispatchEvent(new win.Event('submit',{bubbles:true,cancelable:true}));
assert.equal(win.worldManager.getCharacter().column,9);
function tap(c,r){const target=win.sceneEditing.centre({column:c,row:r}),cam=win.sceneEditing.camera(),zoom=.45*2.5;const x=(160+(target.x-160)*zoom+cam.panX)*2,y=(182+(target.y-182)*zoom+cam.panY)*2;for(const type of ['pointerdown','pointerup']){const e=new win.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,button:0});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'touch'}});canvas.dispatchEvent(e);}}
tap(10,9);assert.equal(win.worldManager.getCharacter().column,10,'gameplay tap moves and persists');assert.equal(win.worldManager.getCharacter().facing,'E');
let active=win.worldManager.getActive(),modified=active.tiles.slice();modified[9*20+11]=3;assert(win.worldManager.updateTiles(active.id,modified));
tap(11,9);assert.equal(win.worldManager.getCharacter().column,10);assert.match(d.getElementById('gameplay-movement-status').textContent,/tidak dapat dicapai/);
d.getElementById('mode-map-editor').click();const before=win.localStorage.getItem('gtlauncher.worlds.v1');tap(11,9);assert.equal(win.localStorage.getItem('gtlauncher.worlds.v1'),before,'editor tap does not initiate gameplay walking');
dom.window.close();console.log('Gameplay routing: 9x9/20x20, obstacles, water, tap, persistence and editor isolation OK');
{
 const run=new JSDOM('<main class="preview-stage" data-mode="gameplay"><canvas id="sky-canvas"></canvas><p id="gameplay-movement-status"></p><button id="mode-map-editor"></button></main>',{url:'https://example.test/',runScripts:'outside-only'}),w=run.window,d=w.document,c=d.querySelector('canvas');
 let frames=[],writes=0,character={visible:true,column:0,row:0,scale:1,facing:'S'},draft=null,world={id:'walk',gridSize:20,tiles:Array(400).fill(1)};
 w.matchMedia=()=>({matches:false});w.requestAnimationFrame=cb=>frames.push(cb);w.GameplayPath=Path;
 w.worldManager={getActive:()=>world,getCharacter:()=>({...character}),getHouse:()=>null,updateCharacter:(id,value)=>{writes++;character=value;w.dispatchEvent(new w.Event('worldchange'));return true;}};
 w.sceneEditing={setGameplayDraft:x=>draft=x,toLocal:(x,y)=>({x,y}),tileAt:p=>({column:p.x,row:p.y})};
 w.eval(fs.readFileSync(path.join(root,'gameplay-movement.js'),'utf8'));
 function tap(x,y){for(const type of ['pointerdown','pointerup']){let e=new w.MouseEvent(type,{bubbles:true,clientX:x,clientY:y});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'touch'}});c.dispatchEvent(e);}}
 const tick=t=>{const cb=frames.shift();assert(cb,'animation frame scheduled');cb(t);};
 tap(4,0);tick(0);tick(150);assert.equal(draft.column,.5);assert.equal(writes,0,'intermediate positions not saved');
 tick(300);tick(480);tick(660);tick(960);assert.equal(writes,1);assert.equal(character.column,4);assert.equal(draft,null);
 tap(5,0);tick(1000);w.dispatchEvent(new w.Event('worldchange'));tick(1150);assert.equal(writes,1,'external world change cancels pending move');assert.equal(draft,null);
 run.window.close();console.log('Gameplay interpolated route timing, one final write and cancellation OK');
}
