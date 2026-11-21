const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{window.regionLabels=[];const orig=CanvasRenderingContext2D.prototype.fillText;CanvasRenderingContext2D.prototype.fillText=function(text,...args){if(this.font.includes('56px') && this.fillStyle==='rgba(0, 0, 0, 0.25)')window.regionLabels.push({text,x:args[0],y:args[1]});return orig.call(this,text,...args);};});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded',timeout:90000});await page.waitForFunction(()=>window.sceneEditing&&window.worldManager);
 const before=await page.evaluate(()=>JSON.stringify(worldManager.getActive()));
 await page.evaluate(()=>{window.regionLabels=[];sceneEditing.setCamera(3,0,0)});assert.equal(await page.evaluate(()=>window.regionLabels.length),0,'overlay absent in gameplay');
 await page.locator('#mode-map-editor').click();
 const labels=await page.evaluate(()=>{window.regionLabels=[];sceneEditing.setCamera(3,0,0);return window.regionLabels;});
 assert.deepEqual(labels.map(x=>Number(x.text)),Array.from({length:25},(_,i)=>i+1));
 assert(labels[1].x>labels[0].x,'numbers increase towards right on first row');assert(labels[5].x<labels[0].x,'next row follows other grid axis');
 const turned=await page.evaluate(()=>{window.regionLabels=[];sceneEditing.turnIsland(1);return window.regionLabels;});assert.deepEqual(turned.map(x=>Number(x.text)),Array.from({length:25},(_,i)=>i+1));assert.notEqual(turned[0].x,labels[0].x);
 await page.locator('#mode-gameplay').click();await page.evaluate(()=>{window.regionLabels=[];sceneEditing.setCamera(3,0,0)});assert.equal(await page.evaluate(()=>window.regionLabels.length),0);
 assert.equal(await page.evaluate(()=>JSON.stringify(worldManager.getActive())),before);assert.deepEqual(errors,[]);
 console.log('Editor-only region overlay, numbering, rotation, non-mutating gameplay: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
