const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview');const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://example.test/',runScripts:'outside-only'}),w=dom.window,d=w.document;
const tiles=Array(625).fill(1);tiles[12*25+14]=0;tiles[13*25+14]=3;
w.localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'home',worlds:[{id:'home',name:'Home',description:'',color:'#a8c795',gridSize:25,tiles,showCharacter:true,characterColumn:7,characterRow:4,characterScale:1,characterFacing:'W',house:{column:4,row:3,scale:1,facing:'S',visible:true}}]}));
for(const dialog of d.querySelectorAll('dialog')){dialog.showModal=()=>dialog.open=true;dialog.close=()=>dialog.open=false;}
d.querySelector('.preview-stage').dataset.mode='gameplay';w.matchMedia=()=>({matches:true});w.requestAnimationFrame=()=>0;w.eval(fs.readFileSync(path.join(root,'worlds.js'),'utf8'));w.GameplayPath=require('../preview/gameplay-path.js');
let local={column:6,row:4};w.sceneEditing={setGameplayDraft(){},setGameplayRoute(){},toLocal:()=>local,tileAt:p=>p,hit:()=>false,houseAt:()=>false};
w.eval(fs.readFileSync(path.join(root,'gameplay-movement.js'),'utf8'));
function tap(column,row){local={column,row};const canvas=d.getElementById('sky-canvas');for(const type of ['pointerdown','pointerup']){const e=new w.MouseEvent(type,{bubbles:true,clientX:1,clientY:1});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'touch'}});canvas.dispatchEvent(e);}}
tap(6,4);assert(d.getElementById('hand-object-dialog').open);assert(d.getElementById('hand-object-take').disabled);d.getElementById('hand-object-move').click();
const before=w.localStorage.getItem('gtlauncher.worlds.v1');tap(13,12);assert.equal(w.localStorage.getItem('gtlauncher.worlds.v1'),before,'holes and water reject move');tap(6,4);assert.equal(w.localStorage.getItem('gtlauncher.worlds.v1'),before,'character blocks house footprint');tap(10,10);assert.equal(w.worldManager.getHouse().column,10);assert.equal(w.worldManager.getHouse().row,10);assert.equal(w.worldManager.getCharacter().column,7);assert.equal(w.worldManager.getActive().tiles[12*25+14],0);
const snapshot=w.worldManager.getHouse();assert(!w.worldManager.moveHouse('home',{...snapshot,column:0},16,16),'stale selection cannot move');assert.equal(w.worldManager.getHouse().column,10);
dom.window.close();console.log('Hand house menu, safe placement, stale guard and persistence: OK');
