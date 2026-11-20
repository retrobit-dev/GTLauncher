const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded',timeout:90000});
 await page.waitForFunction(()=>window.sceneEditing && window.worldManager,{timeout:90000});
 const before=await page.evaluate(()=>JSON.stringify(worldManager.getActive()));
 for(const mode of ['mode-gameplay','mode-map-editor']){
  await page.locator('#'+mode).click({timeout:90000});
  const left=page.locator('#island-turn-left'),right=page.locator('#island-turn-right');
  assert(await left.isVisible());assert(await right.isVisible());
  const initial=await page.evaluate(()=>sceneEditing.camera().quarterTurns);
  await right.click({timeout:90000});assert.equal(await page.evaluate(()=>sceneEditing.camera().quarterTurns),(initial+1)%4);
  await left.click({timeout:90000});assert.equal(await page.evaluate(()=>sceneEditing.camera().quarterTurns),initial);
 }
 assert.equal(await page.evaluate(()=>JSON.stringify(worldManager.getActive())),before);assert.deepEqual(errors,[]);
 console.log('Preview rotation buttons visible and working in gameplay and editor without world mutation: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
