const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const b=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await b.newPage({viewport:{width:1000,height:700},reducedMotion:'no-preference'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const tiles=Array(2500).fill(1);tiles[24*50+24]=3;localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'water-idle',worlds:[{id:'water-idle',name:'Water idle',description:'',color:'#a8c795',gridSize:50,tiles,showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'E',house:null}]}));});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.route('**/archive-api.open-meteo.com/**',r=>r.abort());
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.sceneEditing && worldManager.getActive().id==='water-idle');
 await page.waitForTimeout(2500);assert.equal(await page.evaluate(()=>worldManager.getCharacter().row),24,'must not move before five seconds');
 await page.waitForFunction(()=>{const c=worldManager.getCharacter();return c.row!==24 || c.column!==24},null,{timeout:20000});
 assert.equal(await page.evaluate(()=>{const c=worldManager.getCharacter();return sceneEditing.waterDepth(c.column,c.row)}),0,'arrives on dry land');
 assert.deepEqual(errors,[]);console.log('Preview idle water relocation: OK');
 }finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
