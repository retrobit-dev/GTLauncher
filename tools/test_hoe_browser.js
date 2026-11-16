// Browser regression for tools, turning before walking, and the empty-front-tile fallback.
const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try{
 const page=await browser.newPage({viewport:{width:1000,height:700},hasTouch:true,reducedMotion:'reduce'}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'farm',worlds:[{id:'farm',name:'Farm',description:'',color:'#a8c795',gridSize:25,tiles:Array(625).fill(1),showCharacter:true,characterColumn:12,characterRow:12,characterScale:1,characterFacing:'S',house:null}]})));
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.sceneEditing && window.worldManager);
 const point=async(c,r,dy=0)=>page.evaluate(({c,r,dy})=>{const p=sceneEditing.centre({column:c,row:r}),cam=sceneEditing.camera(),rect=document.querySelector('#sky-canvas').getBoundingClientRect();return {x:rect.left+(160+(p.x-160)*.9+cam.panX)*rect.width/320,y:rect.top+(182+(p.y+dy-182)*.9+cam.panY)*rect.height/200};},{c,r,dy});
 const tap=async(c,r)=>{const p=await point(c,r);await page.mouse.click(p.x,p.y);};
 await page.locator('[data-dock="hoe"]').click();await tap(13,12);
 let character=await page.evaluate(()=>worldManager.getCharacter());assert.equal(character.column,12);assert.equal(character.facing,'E','first tap turns');
 await tap(13,12);character=await page.evaluate(()=>worldManager.getCharacter());assert.equal(character.column,13,'second tap walks onto empty outlined tile');assert.equal(await page.evaluate(()=>worldManager.getActive().tiles[12*25+13]),1);
 await tap(14,12);assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,14,'already facing means first tap walks');
 const head=await point(14,12,-10);await page.mouse.click(head.x,head.y);assert.match(await page.locator('#gameplay-movement-status').innerText(),/tanah pertanian belum tersedia/);
 await page.locator('[data-dock="shovel"]').click();assert.equal(await page.locator('[data-dock="shovel"]').getAttribute('aria-pressed'),'true');await tap(15,12);assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,15,'placeholder tool does not prevent movement');
 await page.locator('[data-dock="shovel"]').click();assert.equal(await page.locator('[data-dock="shovel"]').getAttribute('aria-pressed'),'false');await tap(16,12);assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,16,'movement also works without a tool');
 await page.locator('[data-dock="hoe"]').click();
 for(const [c,r,dir] of [[16,11,'N'],[15,11,'W'],[15,12,'S'],[16,12,'E']]) {
   const before=await page.evaluate(()=>worldManager.getCharacter());await tap(c,r);let turned=await page.evaluate(()=>worldManager.getCharacter());assert.equal(turned.facing,dir,`turn ${dir}`);assert.equal(turned.column,before.column);assert.equal(turned.row,before.row);
   await tap(c,r);const moved=await page.evaluate(()=>worldManager.getCharacter());assert.equal(moved.column,c,`walk ${dir} column`);assert.equal(moved.row,r,`walk ${dir} row`);
 }
 await page.locator('[data-dock="hoe"]').click();const before=await page.evaluate(()=>worldManager.getCharacter());assert.equal(before.facing,'E');await tap(16,11);const direct=await page.evaluate(()=>worldManager.getCharacter());assert.equal(direct.row,11,'with no tool, first tap in a different direction moves immediately');assert.equal(direct.facing,'N');
 assert.deepEqual(errors,[]);console.log('Browser: facing-first movement with hoe, other tools and no tool; empty-front fallback and character action: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
