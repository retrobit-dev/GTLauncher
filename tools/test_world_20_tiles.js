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
const legacy={id:'old',name:'Lama',description:'',color:'#a8c795',showCharacter:false};
const initial=JSON.stringify({worlds:[legacy],activeId:'old'}),{w,d,dom}=setup(initial);
d.getElementById('world-new').click();d.getElementById('world-name').value='Baru';
d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
const world=w.worldManager.getActive();assert.equal(world.gridSize,25);assert.equal(world.tiles.length,625);assert(world.tiles.every(t=>t===1));
assert.deepEqual(JSON.parse(w.localStorage.getItem('gtlauncher.worlds.v1')).worlds[0],legacy,'legacy world unchanged');
assert.equal(d.getElementById('tile-remove'),null,'old right-side tile controls remain removed');
assert.equal(d.getElementById('terrain-draw').closest('.device-frame'),null,'terrain tools are outside the tablet');
assert(d.getElementById('terrain-draw').closest('.editor-right-controls'));assert(d.getElementById('terrain-draw').closest('.device-wrap'));
const tiles=world.tiles.slice();tiles[104]=0;assert.equal(w.worldManager.updateTiles(world.id,tiles),true);
assert.equal(w.worldManager.getActive().tiles[104],0,'existing tile data remains supported');
assert.equal(w.worldManager.updateTiles(world.id,tiles.map((x,i)=>i===312?0:x)),false,'character tile protected');
assert.equal(w.worldManager.updateCharacter(world.id,{visible:true,column:4,row:4,scale:1,facing:'S'}),false,'cannot move character into a missing tile');
d.getElementById('mode-map-editor').click();
const canvas=d.getElementById('sky-canvas');canvas.setPointerCapture=()=>{};
function pointer(type,column,row) {const p=w.sceneEditing.centre({column,row});const cam=w.sceneEditing.camera();const x=160+(p.x-160)*.36*cam.zoom+cam.panX,y=182+(p.y-182)*.36*cam.zoom+cam.panY;const e=new w.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x*2,clientY:y*2,button:0});Object.defineProperties(e,{pointerId:{value:4},pointerType:{value:'touch'}});canvas.dispatchEvent(e);}
function tap(c,r){pointer('pointerdown',c,r);pointer('pointerup',c,r);}
d.getElementById('terrain-material').value='4';d.getElementById('terrain-draw').click();tap(4,4);assert.equal(w.worldManager.getActive().tiles[104],4,'draw stone into empty tile');
d.getElementById('terrain-erase').click();tap(4,4);assert.equal(w.worldManager.getActive().tiles[104],0,'erase tile');
d.getElementById('terrain-draw').click();d.getElementById('terrain-material').value='3';tap(4,4);assert.equal(w.worldManager.getActive().tiles[104],3);
d.getElementById('terrain-pick').click();d.getElementById('terrain-material').value='1';tap(4,4);assert.equal(d.getElementById('terrain-material').value,'3','eyedropper selects water');
d.getElementById('terrain-fill').click();d.getElementById('terrain-material').value='5';tap(4,4);assert.equal(w.worldManager.getActive().tiles[104],5);assert.equal(w.worldManager.getActive().tiles[105],1,'fill limited to contiguous matching material');
d.getElementById('terrain-erase').click();tap(12,12);assert.equal(w.worldManager.getActive().tiles[312],1,'protected character tile cannot be removed');
d.getElementById('terrain-fill').click();d.getElementById('terrain-material').value='2';tap(0,0);assert.equal(w.worldManager.getActive().tiles[0],2);assert.equal(w.worldManager.getActive().tiles[624],2,'fill covers connected region');assert.equal(w.worldManager.getActive().tiles[104],5,'fill preserves distinct material');
assert.equal(w.worldManager.updateCharacter(world.id,{visible:true,column:24,row:24,scale:1,facing:'S'}),true,'20x20 object bounds');
d.querySelector(`[data-action="export"][data-id="${world.id}"]`).click();assert.match(d.getElementById('world-message').textContent,/Android versi terbaru/);
const exported=JSON.parse(d.getElementById('world-export-json').value);assert.equal(exported.format,'gtlauncher-preview-world');assert.equal(exported.version,2);assert.equal(exported.world.tiles[104],5);assert.equal(exported.world.gridSize,25);assert.equal(exported.world.characterColumn,24);assert.equal(exported.world.tiles.length,625);
assert(w.worldManager.tillTile(world.id,23,24),'adjacent grass can be tilled');
assert.equal(w.worldManager.getActive().tiles[24*25+23],6);assert(!w.worldManager.tillTile(world.id,23,24),'already tilled cannot be tilled again');assert(!w.worldManager.tillTile(world.id,0,0),'distant tile cannot be tilled');
d.querySelector(`[data-action="export"][data-id="${world.id}"]`).click();
const farmland=JSON.parse(d.getElementById('world-export-json').value);assert.equal(farmland.version,3);assert.equal(farmland.world.tiles[24*25+23],6);
const exportedText=JSON.stringify(exported);
const imported=setup();imported.w.FileReader=class {readAsText(file){this.result=file.contents;this.onload();}};
const fileInput=imported.d.getElementById('world-import-file');Object.defineProperty(fileInput,'files',{configurable:true,value:[{size:exportedText.length,contents:exportedText}]});fileInput.dispatchEvent(new imported.w.Event('change',{bubbles:true}));
assert.deepEqual(Array.from(imported.w.worldManager.getActive().tiles),exported.world.tiles,'preview export/import round trip');
const beforeInvalid=imported.w.localStorage.getItem('gtlauncher.worlds.v1');
const falselyVersioned=JSON.stringify({...exported,version:1});Object.defineProperty(fileInput,'files',{configurable:true,value:[{size:falselyVersioned.length,contents:falselyVersioned}]});fileInput.dispatchEvent(new imported.w.Event('change',{bubbles:true}));assert.equal(imported.w.localStorage.getItem('gtlauncher.worlds.v1'),beforeInvalid,'old format rejects unsupported terrain');const damaged={...exported,world:{...exported.world,tiles:[1]}};const invalidText=JSON.stringify(damaged);Object.defineProperty(fileInput,'files',{configurable:true,value:[{size:invalidText.length,contents:invalidText}]});fileInput.dispatchEvent(new imported.w.Event('change',{bubbles:true}));assert.equal(imported.w.localStorage.getItem('gtlauncher.worlds.v1'),beforeInvalid,'invalid file cannot overwrite data');imported.dom.window.close();
const saved=w.localStorage.getItem('gtlauncher.worlds.v1');dom.window.close();
const again=setup(saved);assert.equal(again.w.worldManager.getActive().gridSize,25);assert.equal(again.w.worldManager.getCharacter().column,24);assert.equal(again.w.localStorage.getItem('gtlauncher.worlds.v1'),saved,'load is non-destructive');again.dom.window.close();
console.log('25x25 creation, draw/erase/pick/fill, terrain versioning, safeguards, reload and legacy preservation: OK');
