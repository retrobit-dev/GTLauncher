const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const b=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 for(const [reduced,facing] of [['no-preference','E']]) {
 const page=await b.newPage({viewport:{width:1000,height:700},reducedMotion:reduced}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript((facing)=>{
  if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'idle',worlds:[{id:'idle',name:'Idle',description:'',color:'#a8c795',gridSize:50,tiles:Array(2500).fill(1),blockStacks:Array.from({length:2500},()=>[2,2,2,2,1]),showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:facing,house:null}]}));
  window.clockOffset=1000;performance.now=()=>window.clockOffset;
  window.bowDraws=0;window.bowSources=[];window.visibleIdle=false;const draw=CanvasRenderingContext2D.prototype.drawImage;CanvasRenderingContext2D.prototype.drawImage=function(image,...args){if(image?.dataset?.playerIdleBow==='true'){window.bowDraws++;window.bowSources.push(image.src);if(image instanceof HTMLCanvasElement && image.width>0 && image.height>0){const data=image.getContext('2d').getImageData(0,0,image.width,image.height).data;window.visibleIdle=Array.prototype.some.call(data,(_,i)=>i%4===3&&data[i]>0);}}return draw.call(this,image,...args)};
 },facing);
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.route('**/archive-api.open-meteo.com/**',r=>r.abort());
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.sceneEditing && worldManager.getActive().id==='idle');await page.waitForTimeout(1000);
 await page.locator('#mode-map-editor').click();await page.locator('#mode-gameplay').click();await page.waitForTimeout(500);const stored=await page.evaluate(()=>localStorage.getItem('gtlauncher.worlds.v1'));assert.equal(await page.evaluate(()=>window.bowDraws),0);await page.evaluate(()=>window.clockOffset=9500);
 if(reduced==='reduce'){await page.waitForTimeout(500);assert.equal(await page.evaluate(()=>window.bowDraws),0);} else {
  await page.waitForFunction(()=>window.bowDraws>0,null,{timeout:60000});
  assert.ok((await page.evaluate(()=>window.bowSources)).some(src=>src?.includes('player-idle-'+(facing==='N'?'back':'front')+'-concept.png')), 'selected pose for the facing is rendered');
  assert.equal(await page.evaluate(()=>window.visibleIdle),true,'composited pose has visible pixels');await page.evaluate(()=>{window.clockOffset=31500;window.bowSources=[]});
  await page.waitForFunction(()=>window.bowSources.some(src=>src?.includes('-sit-lowering.png')),null,{timeout:60000});
  await page.evaluate(()=>{window.clockOffset=32100;window.bowSources=[]});
  await page.waitForFunction(()=>window.bowSources.some(src=>src?.includes('-sit-nearly.png')),null,{timeout:60000});
  await page.evaluate(()=>{window.clockOffset=33000;window.bowSources=[]});
  await page.waitForFunction(()=>window.bowSources.some(src=>src?.includes('-sitting.png')),null,{timeout:60000});
  assert.ok((await page.evaluate(()=>window.bowSources)).some(src=>src?.includes('player-idle-'+(facing==='N'?'back':'front')+'-sitting.png')), 'sitting pose matches facing');
  await page.evaluate(()=>{sceneEditing.setGameplayDraft({...worldManager.getCharacter(),column:24.2,walkPhase:1});window.bowDraws=0});
  await page.waitForTimeout(800);assert.equal(await page.evaluate(()=>window.bowDraws),0,'walking stops idle pose');
  await page.evaluate(()=>{sceneEditing.setGameplayDraft(null);window.bowDraws=0});await page.waitForTimeout(500);assert.equal(await page.evaluate(()=>window.bowDraws),0,'idle delay restarts after walking');
  await page.emulateMedia({reducedMotion:'reduce'});await page.evaluate(()=>{window.clockOffset=50000;window.bowDraws=0});await page.waitForTimeout(800);assert.equal(await page.evaluate(()=>window.bowDraws),0,'reduced motion disables idle');
  await page.emulateMedia({reducedMotion:'no-preference'});await page.waitForTimeout(800);await page.evaluate(()=>{window.clockOffset=59500;window.bowDraws=0});await page.waitForFunction(()=>window.bowDraws>0,null,{timeout:60000});
 }
 assert.equal(await page.evaluate(()=>localStorage.getItem('gtlauncher.worlds.v1')),stored,'idle animation does not modify world data');assert.deepEqual(errors,[]);await page.close();
 }
 console.log('Browser idle on five-layer world after editor/gameplay switch, visible pixels, movement reset and live motion preference: OK');
}finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
