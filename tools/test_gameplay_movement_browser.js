// Real browser test: upgrade a saved 20x20 Home, then move with touch and mouse.
const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try {
 const page=await browser.newPage({viewport:{width:1000,height:700},hasTouch:true,reducedMotion:'no-preference'}), errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const tiles=Array(400).fill(1);tiles[399]=0;tiles[398]=5;tiles[9*20+8]=0;localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'home',worlds:[{id:'home',name:'Home',description:'',color:'#a8c795',gridSize:20,tiles,showCharacter:true,characterColumn:9,characterRow:9,characterScale:1,characterFacing:'S',house:{column:0,row:0,scale:1,facing:'S',visible:true}}]}));});
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 await page.waitForFunction(()=>window.sceneEditing && window.worldManager && document.querySelector('.preview-stage').dataset.mode==='gameplay');
 const point=async(c,r)=>page.evaluate(({c,r})=>{let p=sceneEditing.centre({column:c,row:r}),cam=sceneEditing.camera(),rect=document.querySelector('#sky-canvas').getBoundingClientRect();return {x:rect.left+(160+(p.x-160)*.9+cam.panX)*rect.width/320,y:rect.top+(182+(p.y-182)*.9+cam.panY)*rect.height/200};},{c,r});
 const state=await page.evaluate(()=>({size:worldManager.getActive().gridSize,tiles:worldManager.getActive().tiles,backup:Object.keys(localStorage).find(k=>k.startsWith('gtlauncher.home-before-25.'))}));
 assert.equal(state.size,25);assert.equal(state.tiles[19*25+19],0);assert.equal(state.tiles[19*25+18],5);assert.equal(state.tiles[9*25+8],0);assert(state.backup);
 const startPosition=await point(9,9);
 const cdp=await page.context().newCDPSession(page);let target=await point(10,9);
 assert.equal(await page.evaluate(({x,y})=>document.elementFromPoint(x,y).id,target),'sky-canvas');
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...target,id:1}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
 try { await page.waitForFunction(()=>worldManager.getCharacter().column===10,null,{timeout:15000}); } catch(e) { console.log('DIAG',await page.evaluate(()=>({char:worldManager.getCharacter(),status:document.querySelector('#gameplay-movement-status').textContent,mode:document.querySelector('.preview-stage').dataset.mode})),errors); throw e; }
 const movedPosition=await point(10,9);assert(Math.hypot(movedPosition.x-startPosition.x,movedPosition.y-startPosition.y)>5,'character visibly changes screen position on a short walk');
 target=await point(10,10);await page.mouse.click(target.x,target.y);
 await page.waitForFunction(()=>worldManager.getCharacter().row===10,null,{timeout:30000});
 assert.deepEqual(errors,[]); console.log('Real browser: migrated Home 20 to 25, visible touch and mouse movement, saved arrival: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
