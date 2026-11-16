// Exercise the real renderer, mode controller and pan controller together.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview');
const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://example.test/',runScripts:'outside-only'});
const w=dom.window,d=w.document;
const ctx=new Proxy({createLinearGradient:()=>({addColorStop(){}}),createRadialGradient:()=>({addColorStop(){}})}, {get:(o,k)=>k in o?o[k]:()=>{},set:(o,k,v)=>(o[k]=v,true)});
w.HTMLCanvasElement.prototype.getContext=()=>ctx;
w.matchMedia=()=>({matches:false});w.requestAnimationFrame=()=>0;w.fetch=async()=>({ok:false});
const canvas=d.getElementById('sky-canvas'),surface=d.getElementById('editor-pan-surface');
canvas.getBoundingClientRect=()=>({left:0,top:0,width:640,height:400});surface.setPointerCapture=()=>{};
for(const name of ['worlds.js','object-editor.js','app.js','preview-modes.js','editor-camera.js'])w.eval(fs.readFileSync(path.join(root,name),'utf8'));
d.getElementById('mode-map-editor').click();const before=w.sceneEditing.toLocal(320,200);d.getElementById('editor-pan').click();
assert.equal(surface.hidden,false);
function pointer(type,x,y){const e=new w.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,button:0});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'touch'}});surface.dispatchEvent(e);}
pointer('pointerdown',10,20);pointer('pointermove',210,120);pointer('pointerup',210,120);
assert.equal(w.sceneEditing.camera().panX,100);assert.equal(w.sceneEditing.camera().panY,50);const after=w.sceneEditing.toLocal(320,200);assert.notEqual(after.x,before.x,'island projection moves with pan');
// A drag started on the pan button continues outside it even without pointer capture.
const button=d.getElementById('editor-pan');button.setPointerCapture=()=>{throw Error('unavailable');};
function on(target,type,x,y){const e=new w.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,button:0});Object.defineProperties(e,{pointerId:{value:2},pointerType:{value:'touch'}});target.dispatchEvent(e);}
on(button,'pointerdown',0,0);on(w,'pointermove',80,40);on(w,'pointerup',80,40);
assert.equal(w.sceneEditing.camera().panX,140);assert.equal(w.sceneEditing.camera().panY,70);
d.getElementById('mode-gameplay').click();assert.equal(surface.hidden,true);
d.getElementById('mode-map-editor').click();assert.equal(w.sceneEditing.camera().panX,140);assert.equal(w.sceneEditing.camera().panY,70);
dom.window.close();console.log('Integrated editor touch pan and mode switching: OK');
