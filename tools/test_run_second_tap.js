const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom'),Path=require('../preview/gameplay-path.js');
const dom=new JSDOM('<div class="home-dock"><button data-dock="hand" aria-pressed="false"></button></div><main class="preview-stage" data-mode="gameplay"><canvas id="sky-canvas"></canvas><p id="gameplay-movement-status"></p><button id="mode-map-editor"></button></main>',{url:'https://example.test/',runScripts:'outside-only'}),w=dom.window,d=w.document,canvas=d.querySelector('canvas');
let q=[],draft=null,writes=0,character={visible:true,column:1,row:1,scale:1,facing:'E'};
w.matchMedia=()=>({matches:false});w.requestAnimationFrame=cb=>q.push(cb);w.GameplayPath=Path;
w.worldManager={getActive:()=>({id:'world',gridSize:50,tiles:Array(2500).fill(1)}),getCharacter:()=>({...character}),getHouse:()=>null,updateCharacter:(id,v)=>{character=v;writes++;return true;}};
w.sceneEditing={setGameplayDraft:v=>draft=v,setGameplayRoute(){},toLocal:(x,y)=>({x,y}),tileAt:p=>({column:p.x,row:p.y}),hit:()=>false};
w.eval(fs.readFileSync(path.join(__dirname,'../preview/gameplay-movement.js'),'utf8'));
function tap(x,y){for(const t of ['pointerdown','pointerup']){const e=new w.MouseEvent(t,{bubbles:true,clientX:x,clientY:y});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'touch'}});canvas.dispatchEvent(e);}}
function tick(t){assert(q.length,'frame exists');q.shift()(t)}
tap(4,1);tick(0);tick(200);assert.equal(draft.running,false);const before=draft.column;
d.querySelector('[data-dock="hand"]').setAttribute('aria-pressed','true');w.dispatchEvent(new w.Event('gameplaytoolchange'));tap(4,1);tick(201);
assert.equal(draft.running,true,'second tap engages running with tool selected');assert(Math.abs(draft.column-before)<.01,'no positional jump when switching speed');
tick(1100);assert.equal(character.column,4,'running arrives before normal 3×650ms');assert.equal(writes,1);assert.equal(draft,null);
character={...character,facing:'E'};d.querySelector('[data-dock="hand"]').setAttribute('aria-pressed','false');tap(6,1);tick(1200);tick(1300);tap(7,1);tick(1301);assert(draft.running);tick(2000);assert.equal(character.column,6,'first destination reached');tick(2001);assert(draft.running,'queued destination continues running');tick(2331);assert.equal(character.column,7);assert.equal(writes,3);
dom.window.close();console.log('Second tap starts continuous running and queues new destination: OK');
