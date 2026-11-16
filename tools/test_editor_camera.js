const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview');
const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://example.test/',runScripts:'outside-only'}),w=dom.window,d=w.document;
const stage=d.querySelector('.preview-stage'),canvas=d.getElementById('sky-canvas');stage.dataset.mode='editor';
canvas.getBoundingClientRect=()=>({width:640,height:400});canvas.setPointerCapture=()=>{};
let camera={zoom:1,panX:0,panY:0}, world={id:'old'};
w.worldManager={getActive:()=>world};w.sceneEditing={camera:()=>({...camera}),setCamera:(z,x,y)=>{camera={zoom:Math.max(.1,Math.min(2,z)),panX:x,panY:y};return true}};
w.eval(fs.readFileSync(path.join(root,'editor-camera.js'),'utf8'));
d.getElementById('editor-zoom-in').click();assert.equal(camera.zoom,1.1);assert.equal(d.getElementById('editor-zoom-value').textContent,'110%');
for(let i=0;i<25;i++)d.getElementById('editor-zoom-in').click();assert.equal(camera.zoom,2);assert(d.getElementById('editor-zoom-in').disabled);
for(let i=0;i<25;i++)d.getElementById('editor-zoom-out').click();assert.equal(camera.zoom,.1);assert(d.getElementById('editor-zoom-out').disabled);
d.getElementById('editor-fit').click();assert.deepEqual(camera,{zoom:1,panX:0,panY:0});
let objectEvents=0;canvas.addEventListener('pointerdown',()=>objectEvents++);d.getElementById('editor-pan').click();
function event(type,x,y){const e=new w.MouseEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,button:0});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'mouse'}});canvas.dispatchEvent(e);}
event('pointerdown',0,0);event('pointermove',1920,-1200);event('pointerup',1920,-1200);
assert.deepEqual(camera,{zoom:1,panX:960,panY:-600});assert.equal(objectEvents,0,'pan gestures do not start object editing');
w.dispatchEvent(new w.Event('worldchange'));assert.equal(camera.panX,960,'ordinary object edits do not reset camera');world={id:'another'};w.dispatchEvent(new w.Event('worldchange'));assert.deepEqual(camera,{zoom:1,panX:0,panY:0});
d.getElementById('mode-gameplay').click();assert.equal(d.getElementById('editor-pan').getAttribute('aria-pressed'),'false');
dom.window.close();console.log('Editor camera zoom, unbounded 3-viewport pan, gesture isolation and reset: OK');
