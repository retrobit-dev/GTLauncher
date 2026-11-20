const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded',timeout:90000});
 await page.waitForFunction(()=>window.sceneEditing && window.worldManager,null,{timeout:90000});
 const before=await page.evaluate(()=>JSON.stringify(worldManager.getActive()));
 for(const mode of ['mode-gameplay','mode-map-editor']){
  await page.locator('#'+mode).click({timeout:90000});
  const left=page.locator('#island-turn-left'),right=page.locator('#island-turn-right');
  assert(await left.isVisible());assert(await right.isVisible());
  const geometry=await page.evaluate(()=>{const r=id=>document.querySelector(id).getBoundingClientRect(), display=r('.device-display'),left=r('#island-turn-left'),right=r('#island-turn-right');const middle=document.elementFromPoint(display.left+display.width/2,display.top+display.height/2);return {display:{left:display.left,right:display.right,top:display.top,height:display.height,width:display.width},left:{left:left.left,right:left.right,top:left.top,height:left.height},right:{left:right.left,right:right.right,top:right.top,height:right.height},middle:middle?.id};});
  const centre=geometry.display.top+geometry.display.height/2;
  assert(Math.abs(geometry.left.top+geometry.left.height/2-centre)<1);assert(Math.abs(geometry.right.top+geometry.right.height/2-centre)<1);
  assert(geometry.left.left<geometry.display.left+geometry.display.width*.1);assert(geometry.right.right>geometry.display.right-geometry.display.width*.1);
  assert.notEqual(geometry.middle,'island-turn-left');assert.notEqual(geometry.middle,'island-turn-right');
  assert.notEqual(geometry.middle,'','rotation container must not intercept the middle of the screen');
  const iconSizes=await page.evaluate(()=>{const l=document.querySelector('#island-turn-left').getBoundingClientRect(),r=document.querySelector('#island-turn-right').getBoundingClientRect(),d=document.querySelector('.home-dock button').getBoundingClientRect(),imgs=[...document.querySelectorAll('.island-turn-controls img')];return {l:{width:l.width,height:l.height},r:{width:r.width,height:r.height},dock:{width:d.width,height:d.height},loaded:imgs.every(i=>i.complete && i.naturalWidth>0),rightMirrored:getComputedStyle(imgs[1]).transform};});
  for(const side of [iconSizes.l,iconSizes.r]){assert(Math.abs(side.width-iconSizes.dock.width)<1);assert(Math.abs(side.height-iconSizes.dock.height)<1);}
  assert(iconSizes.loaded);assert.equal(iconSizes.rightMirrored,'matrix(-1, 0, 0, 1, 0, 0)');
  assert.equal(await left.getAttribute('aria-label'),'Putar pulau 90 derajat ke kanan');
  assert.equal(await right.getAttribute('aria-label'),'Putar pulau 90 derajat ke kiri');
  const initial=await page.evaluate(()=>sceneEditing.camera().quarterTurns);
  await right.click({timeout:90000});assert.equal(await page.evaluate(()=>sceneEditing.camera().quarterTurns),(initial+3)%4);
  await left.click({timeout:90000});assert.equal(await page.evaluate(()=>sceneEditing.camera().quarterTurns),initial);
 }
 assert.equal(await page.evaluate(()=>JSON.stringify(worldManager.getActive())),before);assert.deepEqual(errors,[]);
 console.log('Preview rotation buttons visible and working in gameplay and editor without world mutation: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
