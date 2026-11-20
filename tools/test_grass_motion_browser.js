const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const b=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const p=await b.newPage({viewport:{width:1000,height:700},reducedMotion:'no-preference'});p.setDefaultTimeout(90000);
 await p.route('**/api.open-meteo.com/**',r=>r.abort());
 await p.addInitScript(()=>{let tiles=Array(2500).fill(1);localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'motion',worlds:[{id:'motion',name:'Motion',description:'',color:'#a8c795',gridSize:50,tiles,showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:null,orangeTrees:[{column:26,row:27,kind:'wild-grass',size:1}]}]}));});
 await p.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 await p.waitForFunction(()=>typeof sceneEditing?.plantSway==='function');
 const first=await p.evaluate(()=>{const t=worldManager.getActive().orangeTrees[0];return {s:sceneEditing.plantSway(t),a:sceneEditing.orangeTreeAnchor(t)};});
 await p.waitForFunction(old=>Math.abs(sceneEditing.plantSway(worldManager.getActive().orangeTrees[0])-old)>.35,first.s,{timeout:30000});
 const second=await p.evaluate(()=>sceneEditing.orangeTreeAnchor(worldManager.getActive().orangeTrees[0]));assert.deepEqual(second,first.a,'root stays fixed');
 console.log('Wild grass visible wind motion with fixed root: OK');
}finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
