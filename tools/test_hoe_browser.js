// Real gameplay tool selection, approach, till, persistence, and portable v3 export.
const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try{
 const page=await browser.newPage({viewport:{width:1000,height:700},hasTouch:true,reducedMotion:'no-preference'}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'farm',worlds:[{id:'farm',name:'Farm',description:'',color:'#a8c795',gridSize:25,tiles:Array(625).fill(1),showCharacter:true,characterColumn:12,characterRow:12,characterScale:1,characterFacing:'S',house:null}]}));});
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 await page.waitForFunction(()=>window.sceneEditing && window.worldManager);
 await page.locator('[data-dock="hoe"]').click();assert.equal(await page.locator('[data-dock="hoe"]').getAttribute('aria-pressed'),'true');assert.equal(await page.locator('[data-dock="hand"]').getAttribute('aria-pressed'),'false');
 const point=await page.evaluate(()=>{const p=sceneEditing.centre({column:14,row:12}),c=sceneEditing.camera(),r=document.querySelector('#sky-canvas').getBoundingClientRect();return {x:r.left+(160+(p.x-160)*.9+c.panX)*r.width/320,y:r.top+(182+(p.y-182)*.9+c.panY)*r.height/200};});
 const cdp=await page.context().newCDPSession(page);await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...point,id:1}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
 await page.waitForFunction(()=>document.querySelector('#gameplay-movement-status').textContent.includes('tanah pertanian belum tersedia'));
 assert.equal(await page.evaluate(()=>worldManager.getActive().tiles[12*25+14]),1);
 assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,12);
 await page.locator('[data-dock="hoe"]').click();assert.equal(await page.locator('[data-dock="hoe"]').getAttribute('aria-pressed'),'false');
 await page.mouse.click(point.x,point.y);assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,12,'no tool means no action');
 await page.locator('[data-dock="hand"]').click();await page.mouse.click(point.x,point.y);
 await page.waitForFunction(()=>worldManager.getCharacter().column===14,null,{timeout:30000});
 assert.deepEqual(errors,[]);console.log('Hoe does not change ordinary terrain; toggle and hand movement: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
