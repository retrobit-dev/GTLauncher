const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{if(localStorage.getItem('gtlauncher.worlds.v1'))return;const stacks=Array.from({length:400},()=>[2,2,2,2,1]);const w={id:'gates',name:'Gates',description:'',color:'#a8c795',gridSize:20,tiles:Array(400).fill(1),blockStacks:stacks,showCharacter:true,characterColumn:5,characterRow:5,characterScale:1,characterFacing:'S',house:null};localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'gates',worlds:[w]}));localStorage.setItem('gtlauncher.bridge-redesign-reset.v1','done');});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load',timeout:90000});await page.locator('#mode-map-editor').click();await page.evaluate(()=>sceneEditing.setCamera(3,0,40));
 async function tile(c,r){const p=await page.evaluate(({c,r})=>{const a=document.querySelector('#sky-canvas').getBoundingClientRect(),o=sceneEditing.toLocal(a.left,a.top),z=sceneEditing.camera().renderZoom,p=sceneEditing.centre({column:c,row:r,blockHeight:5});return{x:a.left+(p.x-o.x)*z*a.width/320,y:a.top+(p.y-o.y)*z*a.height/200};},{c,r});await page.mouse.click(p.x,p.y);}
 await page.locator('#object-library-button').click();await page.locator('#catalog-tab-buildings').click();await page.locator('#catalog-place-fence-gate-wood').click();await tile(10,10);
 assert.equal(await page.evaluate(()=>worldManager.getActive().fences[0].material),'gate-wood');
 assert.deepEqual(await page.evaluate(()=>GameplayPath.find(worldManager.getActive(),{column:10,row:10},{column:10,row:11},null)),[{column:10,row:10},{column:10,row:11}]);
 await page.locator('#object-undo').click();assert.equal(await page.evaluate(()=>worldManager.getActive().fences?.length||0),0);await page.locator('#object-redo').click();
 await page.locator('#object-library-button').click();await page.locator('#catalog-tab-buildings').click();await page.locator('#catalog-place-fence-gate-stone').click();await tile(11,10);
 assert.equal(await page.evaluate(()=>worldManager.getActive().fences[1].material),'gate-stone');
 await page.evaluate(()=>{const w=worldManager.getActive(),c=worldManager.getCharacter();worldManager.updateCharacter(w.id,{...c,column:10,row:10});});
 await page.locator('#mode-gameplay').click();await page.evaluate(()=>sceneEditing.setCamera(3,0,40));await tile(10,11);
 await page.waitForFunction(()=>worldManager.getCharacter().column===10 && worldManager.getCharacter().row===11,null,{timeout:12000});
 await page.reload({waitUntil:'load'});assert.equal(await page.evaluate(()=>worldManager.getActive().fences[0].material),'gate-wood');
 await page.locator('#world-button').click();await page.locator('button[data-action="export"][data-id="gates"]').click();const data=JSON.parse(await page.locator('#world-export-json').inputValue());assert.equal(data.version,32);
 const old=structuredClone(data);old.version=31;delete old.world.fences;await page.locator('#world-import-file').setInputFiles({name:'old.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(old))});await page.locator('#world-import-replace').click();assert.equal(await page.evaluate(()=>worldManager.getActive().fences[0].material),'gate-wood');
 await page.locator('#world-import-file').setInputFiles({name:'gates.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(data))});await page.locator('#world-import-replace').click();assert.equal(await page.evaluate(()=>worldManager.getActive().fences.length),2);
 assert.deepEqual(errors,[]);console.log('Gate placement, routing, undo, persistence and downgrade: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
