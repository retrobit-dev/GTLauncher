const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});
 const result=await page.evaluate(()=>{const world=worldManager.getActive(),before=JSON.stringify(world),coords=[{column:23,row:24},{column:25,row:26}],steps=[];
 for(let q=0;q<4;q++){steps.push(coords.map(p=>{const centre=sceneEditing.centre(p);return {p,centre,picked:sceneEditing.tileAt(centre)};}));sceneEditing.turnIsland(1);}
 return {steps,unchanged:before===JSON.stringify(world),turns:sceneEditing.camera().quarterTurns};});
 for(const step of result.steps)for(const item of step)assert.deepEqual(item.picked,item.p);assert(result.unchanged);assert.equal(result.turns,0);assert.deepEqual(errors,[]);console.log('Experimental preview rotation: tile centres and picking round-trip without world mutation: OK');await page.close();
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
