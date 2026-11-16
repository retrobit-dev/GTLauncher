// Exercise the hand tool with a real transparent house sprite in the preview.
const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try{
 const page=await browser.newPage({viewport:{width:1000,height:700},hasTouch:true,reducedMotion:'reduce'});
 await page.addInitScript(()=>{if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'home',worlds:[{id:'home',name:'Home',description:'',color:'#a8c795',gridSize:25,tiles:Array(625).fill(1),showCharacter:true,characterColumn:8,characterRow:4,characterScale:1,characterFacing:'S',house:{column:4,row:2,scale:1,facing:'S',visible:true}}]}));});
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 await page.waitForFunction(()=>window.sceneEditing && worldManager.getHouse() && sceneEditing.houseAt);
 assert.equal(await page.locator('[data-dock="hand"]').getAttribute('aria-pressed'),'true');assert.equal(await page.locator('#dock-dialog').evaluate(e=>e.open),false);
 await page.waitForFunction(()=>{const h=worldManager.getHouse(), p=sceneEditing.centreHouse(h);for(let y=p.y-50;y<p.y;y+=2)for(let x=p.x-30;x<p.x+30;x+=2)if(sceneEditing.houseAt({x,y},h))return true;return false;});
 const target=await page.evaluate(()=>{const h=worldManager.getHouse(),p=sceneEditing.centreHouse(h),c=sceneEditing.camera(),r=document.querySelector('#sky-canvas').getBoundingClientRect();for(let y=p.y-50;y<p.y;y+=2)for(let x=p.x-30;x<p.x+30;x+=2)if(sceneEditing.houseAt({x,y},h)){return {x:r.left+(160+(x-160)*.9+c.panX)*r.width/320,y:r.top+(182+(y-182)*.9+c.panY)*r.height/200};}});
 assert.equal(await page.evaluate(({x,y})=>document.elementFromPoint(x,y).id,target),'sky-canvas');
 await page.mouse.click(target.x,target.y);assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,8,'first tap turns toward the house');
 await page.mouse.click(target.x,target.y);
 let character=await page.evaluate(()=>worldManager.getCharacter());
 const dx=Math.max(4-character.column,0,character.column-6),dy=Math.max(2-character.row,0,character.row-4);
 assert.equal(dx+dy,1,'character approaches house footprint');
 const front=await page.evaluate(()=>{const c=worldManager.getCharacter();return {column: c.column-1,row:c.row};});
 const frontPoint=await page.evaluate(f=>{let p=sceneEditing.centre(f),cam=sceneEditing.camera(),r=document.querySelector('#sky-canvas').getBoundingClientRect();return {x:r.left+(160+(p.x-160)*.9+cam.panX)*r.width/320,y:r.top+(182+(p.y-182)*.9+cam.panY)*r.height/200}},front);
 await page.mouse.click(frontPoint.x,frontPoint.y);assert.match(await page.locator('#gameplay-movement-status').innerText(),/Rumah dipilih/i);
 assert(await page.locator('#hand-object-dialog').evaluate(e=>e.open));assert(await page.locator('#hand-object-take').isDisabled());await page.locator('#hand-object-move').click();
 const placement=await page.evaluate(()=>{let p=sceneEditing.centre({column:10,row:7}),cam=sceneEditing.camera(),r=document.querySelector('#sky-canvas').getBoundingClientRect();return {x:r.left+(160+(p.x-160)*.9+cam.panX)*r.width/320,y:r.top+(182+(p.y-182)*.9+cam.panY)*r.height/200}});await page.mouse.click(placement.x,placement.y);
 assert.equal((await page.evaluate(()=>worldManager.getHouse())).column,10);assert.equal((await page.evaluate(()=>worldManager.getHouse())).row,7);assert.match(await page.locator('#gameplay-movement-status').innerText(),/berhasil dipindahkan/i);
 await page.reload({waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.worldManager);assert.equal((await page.evaluate(()=>worldManager.getHouse())).column,10,'house position persists');
 console.log('Hand selection and opaque house hit approach: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
