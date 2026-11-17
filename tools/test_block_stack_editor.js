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
d.getElementById('world-new').click();d.getElementById('world-name').value='Stack gestures';d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
d.getElementById('mode-map-editor').click();const canvas=d.getElementById('sky-canvas');canvas.setPointerCapture=()=>{};
function pointer(type,column,row){const p=w.sceneEditing.centre({column,row}),cam=w.sceneEditing.camera(),x=160+(p.x-160)*cam.renderZoom+cam.panX,y=182+(p.y-182)*cam.renderZoom+cam.panY;const e=new w.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x*2,clientY:y*2,button:0});Object.defineProperties(e,{pointerId:{value:4},pointerType:{value:'touch'}});canvas.dispatchEvent(e);}
function tap(c,r){pointer('pointerdown',c,r);pointer('pointerup',c,r);}
function stack(c,r){let world=w.worldManager.getActive(),i=r*50+c;return Array.from(world.blockStacks?.[i] || (world.tiles[i]?[world.tiles[i]]:[]));}
d.getElementById('terrain-add').click();d.getElementById('terrain-material').value='2';
pointer('pointerdown',4,4);pointer('pointermove',8,8);pointer('pointermove',4,4);pointer('pointerup',4,4);
assert.deepEqual(stack(4,4),[1,2]);assert.deepEqual(stack(8,8),[1,2]);
d.getElementById('object-undo').click();assert.deepEqual(stack(4,4),[1]);assert.deepEqual(stack(8,8),[1]);
d.getElementById('object-redo').click();assert.deepEqual(stack(4,4),[1,2]);assert.deepEqual(stack(8,8),[1,2]);
d.getElementById('terrain-draw').click();d.getElementById('terrain-material').value='4';tap(4,4);assert.deepEqual(stack(4,4),[1,4]);
d.getElementById('terrain-erase').click();tap(4,4);assert.deepEqual(stack(4,4),[1]);
d.getElementById('object-undo').click();assert.deepEqual(stack(4,4),[1,4]);
d.getElementById('terrain-pick').click();d.getElementById('terrain-material').value='1';tap(4,4);assert.equal(d.getElementById('terrain-material').value,'4');assert.equal(d.getElementById('block-picker-value').textContent,'Batu','eyedropper updates visible picker');assert.equal(d.querySelector('[data-block-value="4"]').getAttribute('aria-selected'),'true');
// Out-of-band stack changes invalidate history even when the top material is unchanged.
const world=w.worldManager.getActive(),changed=world.blockStacks.map(x=>x.slice());changed[8*50+8][0]=3;
assert(w.worldManager.updateBlockStacks(world.id,changed));assert(d.getElementById('object-undo').disabled);
dom.window.close();console.log('Stack gestures, top-only operations, grouped history and external edits: OK');
