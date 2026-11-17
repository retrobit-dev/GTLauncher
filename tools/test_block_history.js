const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview'), html=fs.readFileSync(path.join(root,'index.html'),'utf8');
function setup(raw) {
 const dom=new JSDOM(html,{url:'https://example.test/',runScripts:'outside-only'}),w=dom.window,d=w.document;
 if(raw)w.localStorage.setItem('gtlauncher.worlds.v1',raw);
 for(const dialog of d.querySelectorAll('dialog')){dialog.showModal=()=>dialog.open=true;dialog.close=()=>dialog.open=false;}
 const ctx=new Proxy({createLinearGradient:()=>({addColorStop(){}}),createRadialGradient:()=>({addColorStop(){}})}, {get:(o,k)=>k in o?o[k]:()=>{},set:(o,k,v)=>(o[k]=v,true)});
 w.URL.createObjectURL=()=> 'blob:test';w.URL.revokeObjectURL=()=>{};
 w.HTMLCanvasElement.prototype.getContext=()=>ctx;w.matchMedia=()=>({matches:false});w.requestAnimationFrame=()=>0;w.fetch=async()=>({ok:false});
 d.getElementById('sky-canvas').getBoundingClientRect=()=>({left:0,top:0,width:640,height:400});
 for(const name of ['worlds.js','object-editor.js','app.js','preview-modes.js','editor-camera.js','terrain-tools.js'])w.eval(fs.readFileSync(path.join(root,name),'utf8'));
 return {w,d,dom};
}

const {w,d,dom}=setup();
d.getElementById('world-new').click();d.getElementById('world-name').value='Block history';
d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
d.getElementById('mode-map-editor').click();
const world=w.worldManager.getActive(),canvas=d.getElementById('sky-canvas');canvas.setPointerCapture=()=>{};
const undo=d.getElementById('object-undo'),redo=d.getElementById('object-redo');
function pointer(type,column,row) {const p=w.sceneEditing.centre({column,row}),cam=w.sceneEditing.camera(),x=160+(p.x-160)*cam.renderZoom+cam.panX,y=182+(p.y-182)*cam.renderZoom+cam.panY;const e=new w.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x*2,clientY:y*2,button:0});Object.defineProperties(e,{pointerId:{value:4},pointerType:{value:'touch'}});canvas.dispatchEvent(e);}
function tap(c,r){pointer('pointerdown',c,r);pointer('pointerup',c,r);}
function tile(c,r){return w.worldManager.getActive().tiles[r*50+c]}
assert(undo.disabled && redo.disabled);
d.getElementById('terrain-material').value='2';d.getElementById('terrain-draw').click();
pointer('pointerdown',4,4);pointer('pointermove',5,4);pointer('pointerup',5,4);
assert.equal(tile(4,4),2);assert.equal(tile(5,4),2);assert(!undo.disabled && redo.disabled);
undo.click();assert.equal(tile(4,4),1);assert.equal(tile(5,4),1);assert(undo.disabled && !redo.disabled,'one drag is one history step');
redo.click();assert.equal(tile(4,4),2);assert.equal(tile(5,4),2);
// Selection and block history use the same chronological stack.
d.getElementById('object-select-button').click();d.getElementById('object-details-button').click();
d.getElementById('object-column').value='23';d.getElementById('object-row').value='24';
d.getElementById('object-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
assert.equal(w.worldManager.getCharacter().column,23);
undo.click();assert.equal(w.worldManager.getCharacter().column,24);assert.equal(tile(4,4),2);
undo.click();assert.equal(tile(4,4),1);assert.equal(tile(5,4),1);
redo.click();assert.equal(tile(4,4),2);redo.click();assert.equal(w.worldManager.getCharacter().column,23);
// Add on an occupied cell creates another level, and history restores all levels.
d.getElementById('terrain-add').click();tap(4,4);assert.deepEqual(Array.from(w.worldManager.getActive().blockStacks[204]),[2,2]);
undo.click();assert.equal(w.worldManager.getActive().blockStacks,undefined);redo.click();assert.deepEqual(Array.from(w.worldManager.getActive().blockStacks[204]),[2,2]);
d.getElementById('terrain-erase').click();tap(4,4);assert.equal(tile(4,4),2);tap(4,4);assert.equal(tile(4,4),0);
d.getElementById('terrain-draw').click();tap(4,4);assert.equal(tile(4,4),0);assert.match(d.getElementById('terrain-status').textContent,/Sel kosong/);
d.getElementById('terrain-add').click();tap(4,4);assert.equal(tile(4,4),2);undo.click();assert.equal(tile(4,4),0);redo.click();assert.equal(tile(4,4),2);
// A successful new action invalidates redo, while a rejected action leaves it intact.
undo.click();assert(!redo.disabled);d.getElementById('terrain-draw').click();tap(4,4);assert(!redo.disabled);d.getElementById('terrain-add').click();tap(4,4);assert(redo.disabled);
// Out-of-band changes must invalidate history instead of overwriting them.
const external=w.worldManager.getActive().tiles.slice();external[7*50+7]=5;assert(w.worldManager.updateTiles(world.id,external));assert(undo.disabled && redo.disabled);assert.equal(tile(7,7),5);
// Stale-tab writes are refused during undo; neither newer data nor in-memory tiles are overwritten.
d.getElementById('terrain-draw').click();d.getElementById('terrain-material').value='4';tap(8,8);assert.equal(tile(8,8),4);assert(!undo.disabled);
const remote=JSON.parse(w.localStorage.getItem('gtlauncher.worlds.v1'));remote.worlds.find(x=>x.id===world.id).tiles[9*50+9]=3;
const remoteRaw=JSON.stringify(remote);w.localStorage.setItem('gtlauncher.worlds.v1',remoteRaw);undo.click();
assert.equal(w.localStorage.getItem('gtlauncher.worlds.v1'),remoteRaw,'undo cannot overwrite another tab');assert.equal(tile(8,8),4,'failed undo leaves current state intact');assert(!undo.disabled);
const saved=remoteRaw;dom.window.close();
const loaded=setup(saved);assert.equal(loaded.w.worldManager.getActive().tiles[7*50+7],5,'world is persistent');loaded.dom.window.close();
console.log('Block add/replace, grouped undo/redo, object ordering and conflict handling: OK');
