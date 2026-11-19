const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'pet',worlds:[{id:'pet',name:'Pet',description:'',color:'#a8c795',gridSize:50,tiles:Array(2500).fill(1),showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:null}]}));window.clockOffset=0;const realNow=performance.now.bind(performance);performance.now=()=>realNow()+window.clockOffset;window.petDraws=[];const draw=CanvasRenderingContext2D.prototype.drawImage;CanvasRenderingContext2D.prototype.drawImage=function(image,...args){if(image?.src?.includes('pet-sparrow.png'))window.petDraws.push(args);return draw.call(this,image,...args)};});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.route('**/archive-api.open-meteo.com/**',r=>r.abort());
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.petDraws.length>0,null,{timeout:60000});
 let ground=await page.evaluate(()=>window.petDraws.at(-1));
 await page.evaluate(()=>{window.clockOffset=34000;window.petDraws=[]});await page.waitForTimeout(1500);
 if(process.env.SPARROW_SCREENSHOT)await page.screenshot({path:process.env.SPARROW_SCREENSHOT});
 const perched=await page.evaluate(()=>window.petDraws.at(-1));assert.ok(perched[1]<ground[1]-15,'bird perches above ground when player sits');
 await page.evaluate(()=>{sceneEditing.setGameplayDraft({...worldManager.getCharacter(),column:24.1,walkPhase:1});window.clockOffset=35000;window.petDraws=[]});await page.waitForFunction(()=>window.petDraws.length>2,null,{timeout:60000});
 assert.deepEqual(errors,[]);console.log('Preview sparrow rendered and perches when player sits: OK');
 }finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
