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
d.getElementById('world-new').click();d.getElementById('world-name').value='Stack persistence';
d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
const world=w.worldManager.getActive(), size=world.gridSize;
const stacks=world.tiles.map(t=>t?[t]:[]), at=4*size+4, character=24*size+24;
stacks[at].push(2);assert(w.worldManager.updateBlockStacks(world.id,stacks));
assert.deepEqual(Array.from(w.worldManager.getActive().blockStacks[at]),[1,2]);assert.equal(w.worldManager.getActive().tiles[at],2);
stacks[at].push(3);assert.equal(w.worldManager.getActive().blockStacks[at].length,2,'saved data is copied');
const invalid=w.worldManager.getActive().blockStacks.map(x=>x.slice());invalid[at]=[0];assert(!w.worldManager.updateBlockStacks(world.id,invalid));
const occupied=w.worldManager.getActive().blockStacks.map(x=>x.slice());occupied[character].push(1);assert(!w.worldManager.updateBlockStacks(world.id,occupied),'character support height protected');
const flat=w.worldManager.getActive().tiles.slice();flat[at]=1;assert(!w.worldManager.updateTiles(world.id,flat),'legacy writes cannot flatten stacked terrain');
const saved=w.localStorage.getItem('gtlauncher.worlds.v1');dom.window.close();
const loaded=setup(saved);assert.deepEqual(Array.from(loaded.w.worldManager.getActive().blockStacks[at]),[1,2]);assert.equal(loaded.w.worldManager.getActive().tiles[at],2);
const broken=JSON.parse(saved);broken.worlds[0].blockStacks[at]=[3];const damaged=setup(JSON.stringify(broken));assert.equal(damaged.w.worldManager.getActive().id,'base','inconsistent data not accepted');
assert.equal(damaged.w.localStorage.getItem('gtlauncher.worlds.v1'),JSON.stringify(broken),'invalid data never overwritten');
loaded.dom.window.close();damaged.dom.window.close();console.log('Stack persistence, validation, legacy safety and reload: OK');
