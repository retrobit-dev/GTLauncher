const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview');
function make(reduced){
 const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://example.test/',runScripts:'outside-only'}),w=dom.window,d=w.document;
 let now=0;w.performance.now=()=>now;w.matchMedia=()=>({matches:reduced});w.requestAnimationFrame=()=>0;w.fetch=async()=>({ok:false});
 const ctx=new Proxy({createLinearGradient:()=>({addColorStop(){}}),createRadialGradient:()=>({addColorStop(){}})}, {get:(o,k)=>k in o?o[k]:()=>{},set:(o,k,v)=>(o[k]=v,true)});
 w.HTMLCanvasElement.prototype.getContext=()=>ctx;d.getElementById('sky-canvas').getBoundingClientRect=()=>({left:0,top:0,width:640,height:400});
 w.worldManager={getActive:()=>({id:'x',gridSize:50,tiles:Array(2500).fill(1)}),getCharacter:()=>({visible:true,column:24,row:24,scale:1,facing:'S'})};
 w.eval(fs.readFileSync(path.join(root,'app.js'),'utf8'));return {dom,w,advance:ms=>{now+=ms}};
}
const run=make(false),scene=run.w.sceneEditing;
const start=scene.camera();run.advance(16);scene.setGameplayDraft({visible:true,column:25,row:24,scale:1,facing:'E'});
let first=scene.camera();assert(first.panX<start.panX,'camera begins following');assert(Math.abs(first.panX-start.panX)<15,'first follow step is eased, not snapped');
run.advance(16);scene.setGameplayDraft({visible:true,column:25,row:24,scale:1,facing:'E'});let second=scene.camera();assert(second.panX<first.panX,'camera continues moving');
for(let i=0;i<100;i++){run.advance(16);scene.setGameplayDraft({visible:true,column:25,row:24,scale:1,facing:'E'});}
const settled=scene.camera(),tile=scene.centre({column:25,row:24});
const sx=(160+(tile.x-160)*settled.renderZoom+settled.panX)*2,sy=(182+(tile.y-182)*settled.renderZoom+settled.panY)*2,local=scene.toLocal(sx,sy);
assert(Math.abs(local.x-tile.x)<1e-7 && Math.abs(local.y-tile.y)<1e-7,'inverse hit testing uses same eased camera');
scene.setMode('editor');scene.setCamera(3,20,30);scene.setMode('gameplay');scene.setMode('editor');assert.equal(scene.camera().zoom,3);assert.equal(scene.camera().panX,20);run.dom.window.close();
const reduced=make(true);const before=reduced.w.sceneEditing.camera();reduced.w.sceneEditing.setGameplayDraft({visible:true,column:25,row:24,scale:1,facing:'E'});assert(Math.abs(reduced.w.sceneEditing.camera().panX-before.panX)>3,'reduced motion snaps rather than animating');reduced.dom.window.close();
console.log('Smooth follow, settle, inverse hit tests, editor restore and reduced motion: OK');
