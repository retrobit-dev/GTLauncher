const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview');
const dom=new JSDOM('<div class="home-dock"></div><main class="preview-stage" data-mode="gameplay"><canvas id="sky-canvas"></canvas><p id="gameplay-movement-status"></p><button id="mode-map-editor"></button></main>',{url:'https://example.test/',runScripts:'outside-only'}),w=dom.window,d=w.document,c=d.querySelector('canvas');
let frames=[],draft=null,writes=0,character={visible:true,column:5,row:5,scale:1,facing:'S'};
const route=[{column:5,row:5},{column:6,row:5},{column:6,row:4},{column:5,row:4},{column:5,row:5}];
w.matchMedia=()=>({matches:false});w.requestAnimationFrame=cb=>frames.push(cb);w.GameplayPath={find:()=>route};
w.worldManager={getActive:()=>({id:'world'}),getCharacter:()=>({...character}),getHouse:()=>null,updateCharacter:(id,v)=>{writes++;character=v;return true;}};
w.sceneEditing={setGameplayDraft:v=>draft=v,setGameplayRoute(){},toLocal:(x,y)=>({x,y}),tileAt:p=>({column:p.x,row:p.y})};
w.eval(fs.readFileSync(path.join(root,'gameplay-movement.js'),'utf8'));
for(const type of ['pointerdown','pointerup']){let e=new w.MouseEvent(type,{bubbles:true,clientX:5,clientY:5});Object.defineProperties(e,{pointerId:{value:1},pointerType:{value:'touch'}});c.dispatchEvent(e);}
function tick(t){assert(frames.length);frames.shift()(t)}
tick(0);assert.equal(draft.facing,'E');assert.equal(draft.walkPhase,0);
tick(75);assert.equal(draft.facing,'E');assert(Math.abs(Math.sin(draft.walkPhase)-1)<1e-9,'gait reaches opposite limb swing');
tick(300);assert.equal(draft.facing,'N');
tick(480);assert.equal(draft.facing,'W');
tick(660);assert.equal(draft.facing,'S');
tick(960);assert.equal(draft,null);assert.equal(character.facing,'S');assert.equal(writes,1,'only final position persisted');
const app=fs.readFileSync(path.join(root,'app.js'),'utf8');assert(app.includes("const mirror = character.facing === 'W' || character.facing === 'E'"));assert(app.includes('ctx.rotate(side*phase*.10)'));assert(app.includes('shift=phase*.8*character.scale'));
const java=fs.readFileSync(path.join(root,'../app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java'),'utf8');assert(java.includes('boolean mirror = "W".equals(characterFacing) || "E".equals(characterFacing)'));assert(java.includes('width/2-armW/2,topH,armW,armH,-angle,-phase,runBlend,false'));
dom.window.close();console.log('Facing follows each route segment; gait alternates and Android mapping matches: OK');
