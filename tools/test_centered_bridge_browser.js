const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const page=await browser.newPage({viewport:{width:1200,height:800},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const tiles=Array(2500).fill(1),blockStacks=tiles.map(()=>[1]);if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'test',worlds:[{id:'test',name:'Test',description:'',color:'#a8c795',gridSize:50,tiles,blockStacks,showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:null}]}));});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded',timeout:90000});await page.waitForFunction(()=>window.worldManager&&window.sceneEditing);
 const result=await page.evaluate(()=>{const bridge={column:20,row:20,axis:'E',material:'wood',gap:true,free:true,deck:1,centered:true};const placed=worldManager.replaceBridges('test',[],[bridge]);const world=worldManager.getActive();return {placed,bridge:world.bridges?.[0],center:BridgeGeometry.deckHeight(world,{column:21,row:20}),side:BridgeGeometry.deckHeight(world,{column:21,row:19})};});
 assert(result.placed);assert(result.bridge.centered);assert.equal(result.center,1);assert.equal(result.side,null);
 await page.reload({waitUntil:'domcontentloaded',timeout:90000});await page.waitForFunction(()=>window.worldManager);assert((await page.evaluate(()=>worldManager.getActive().bridges[0])).centered);assert.deepEqual(errors,[]);
 console.log('Centered bridge persists and keeps the central walkable route: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
