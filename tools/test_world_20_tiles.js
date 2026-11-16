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
 for(const name of ['worlds.js','object-editor.js','app.js','preview-modes.js','editor-camera.js'])w.eval(fs.readFileSync(path.join(root,name),'utf8'));
 return {w,d,dom};
}
const legacy={id:'old',name:'Lama',description:'',color:'#a8c795',showCharacter:false};
const initial=JSON.stringify({worlds:[legacy],activeId:'old'}),{w,d,dom}=setup(initial);
d.getElementById('world-new').click();d.getElementById('world-name').value='Baru';
d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
const world=w.worldManager.getActive();assert.equal(world.gridSize,20);assert.equal(world.tiles.length,400);assert(world.tiles.every(t=>t===1));
assert.deepEqual(JSON.parse(w.localStorage.getItem('gtlauncher.worlds.v1')).worlds[0],legacy,'legacy world unchanged');
assert.equal(d.getElementById('tile-remove'),null,'right-side tile controls removed');
const tiles=world.tiles.slice();tiles[84]=0;assert.equal(w.worldManager.updateTiles(world.id,tiles),true);
assert.equal(w.worldManager.getActive().tiles[84],0,'existing tile data remains supported');
assert.equal(w.worldManager.updateTiles(world.id,tiles.map((x,i)=>i===189?0:x)),false,'character tile protected');
assert.equal(w.worldManager.updateCharacter(world.id,{visible:true,column:4,row:4,scale:1,facing:'S'}),false,'cannot move character into a missing tile');
assert.equal(w.worldManager.updateCharacter(world.id,{visible:true,column:19,row:19,scale:1,facing:'S'}),true,'20x20 object bounds');
d.querySelector(`[data-action="export"][data-id="${world.id}"]`).click();assert.match(d.getElementById('world-message').textContent,/hanya untuk preview/);
const exported=JSON.parse(d.getElementById('world-export-json').value);assert.equal(exported.format,'gtlauncher-preview-world');assert.equal(exported.world.gridSize,20);assert.equal(exported.world.characterColumn,19);assert.equal(exported.world.tiles.length,400);
const exportedText=JSON.stringify(exported);
const imported=setup();imported.w.FileReader=class {readAsText(file){this.result=file.contents;this.onload();}};
const fileInput=imported.d.getElementById('world-import-file');Object.defineProperty(fileInput,'files',{configurable:true,value:[{size:exportedText.length,contents:exportedText}]});fileInput.dispatchEvent(new imported.w.Event('change',{bubbles:true}));
assert.deepEqual(Array.from(imported.w.worldManager.getActive().tiles),exported.world.tiles,'preview export/import round trip');
const beforeInvalid=imported.w.localStorage.getItem('gtlauncher.worlds.v1');const damaged={...exported,world:{...exported.world,tiles:[1]}};const invalidText=JSON.stringify(damaged);Object.defineProperty(fileInput,'files',{configurable:true,value:[{size:invalidText.length,contents:invalidText}]});fileInput.dispatchEvent(new imported.w.Event('change',{bubbles:true}));assert.equal(imported.w.localStorage.getItem('gtlauncher.worlds.v1'),beforeInvalid,'invalid file cannot overwrite data');imported.dom.window.close();
const saved=w.localStorage.getItem('gtlauncher.worlds.v1');dom.window.close();
const again=setup(saved);assert.equal(again.w.worldManager.getActive().gridSize,20);assert.equal(again.w.worldManager.getCharacter().column,19);assert.equal(again.w.localStorage.getItem('gtlauncher.worlds.v1'),saved,'load is non-destructive');again.dom.window.close();
console.log('20x20 creation, tile preservation, safeguards, reload and legacy preservation: OK');
