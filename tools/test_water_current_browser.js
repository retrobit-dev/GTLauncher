const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'no-preference'}),errors=[];page.setDefaultTimeout(60000);page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const stacks=Array.from({length:2500},()=>[1]);stacks[25*50+25]=[1,3];stacks[25*50+26]=[3];stacks[25*50+27]=[3];let tiles=stacks.map(s=>s.at(-1));localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'flow',worlds:[{id:'flow',name:'Flow',description:'',color:'#a8c795',gridSize:50,tiles,blockStacks:stacks,showCharacter:true,characterColumn:20,characterRow:20,characterScale:1,characterFacing:'S',house:null}]}));});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.locator('#mode-map-editor').click();await page.evaluate(()=>sceneEditing.setCamera(3,0,-40));
 assert.equal(await page.evaluate(()=>WaterFlow.direction(worldManager.getActive(),25,25)),'E');assert.equal((await page.evaluate(()=>sceneEditing.waterFalls(25,25))).right,1);
 const pts=await page.evaluate(()=>{let w=worldManager.getActive(),f=WaterFlow.direction(w,26,25),r=sceneEditing.waterFalls(25,25);return {f,r}});assert.equal(pts.f,'E');assert.deepEqual(errors,[]);
 if(process.env.FLOW_SCREENSHOT){await page.waitForTimeout(1000);await page.screenshot({path:process.env.FLOW_SCREENSHOT});}
 console.log('Browser flowing water direction and downhill slope detection: OK');
 }finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
