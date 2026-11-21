const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const page=await browser.newPage({viewport:{width:1200,height:800},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const tiles=Array(2500).fill(1),blockStacks=tiles.map(()=>[1]);if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'test',worlds:[{id:'test',name:'Test',description:'',color:'#a8c795',gridSize:50,tiles,blockStacks,showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:null}]}));});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded',timeout:90000});await page.waitForFunction(()=>window.worldManager&&window.sceneEditing);
 await page.locator('#mode-map-editor').click();
 await page.evaluate(()=>sceneEditing.setCamera(3,0,80));
 await page.locator('#object-library-button').click();await page.locator('#catalog-place-bridge-wood').click();
 const pos=await page.evaluate(()=>{const r=document.querySelector('#sky-canvas').getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom,p=sceneEditing.centre({column:20,row:20});return{x:r.left+(p.x-o.x)*z*r.width/320,y:r.top+(p.y-o.y)*z*r.height/200}});
 await page.mouse.click(pos.x,pos.y);
 const result=await page.evaluate(()=>{const world=worldManager.getActive();const placed=Boolean(world.bridges?.length);return {placed,bridge:world.bridges?.[0],center:BridgeGeometry.deckHeight(world,{column:21,row:20}),side:BridgeGeometry.deckHeight(world,{column:21,row:19})};});
 assert(result.placed);assert(result.bridge.straight);assert.equal(result.center,1);assert.equal(result.side,null);
 const bridgeDepth=await page.evaluate(()=>{const w=worldManager.getActive(),road={column:21,row:20,material:'water',direction:'S'};if(!worldManager.replaceRoads(w.id,w.roads||[],[road]))throw new Error('road under bridge rejected');const depth=sceneEditing.roadDepth(21,20);if(!worldManager.replaceRoads(w.id,worldManager.getActive().roads,[]))throw new Error('cleanup failed');return depth;});assert.equal(bridgeDepth,0,'character remains at bridge height');
 await page.reload({waitUntil:'domcontentloaded',timeout:90000});await page.waitForFunction(()=>window.worldManager);assert((await page.evaluate(()=>worldManager.getActive().bridges[0])).straight);await page.locator('#world-button').click();await page.locator('button[data-action="export"][data-id="test"]').click();
 const exported=JSON.parse(await page.locator('#world-export-json').inputValue());assert.equal(exported.version,28);assert(exported.world.bridges[0].straight);assert.deepEqual(errors,[]);
 console.log('Straight bridge persists and keeps the central walkable route: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
