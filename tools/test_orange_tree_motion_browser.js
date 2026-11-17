const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try {for(const reduced of ['no-preference','reduce']) {
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:reduced});
 await page.addInitScript(()=>{if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'orchard',worlds:[{id:'orchard',name:'Orchard',description:'',color:'#a8c795',gridSize:50,tiles:Array(2500).fill(1),showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:null,orangeTrees:[{column:26,row:27}]}]}))});
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.locator('#mode-map-editor').click();await page.evaluate(()=>sceneEditing.setCamera(3,0,40));
 await page.waitForFunction(()=>{let t=worldManager.getActive().orangeTrees[0],p=sceneEditing.orangeTreeAnchor(t);for(let y=p.y-55;y<p.y-10;y+=2)for(let x=p.x-25;x<p.x+25;x+=2)if(sceneEditing.orangeTreeAt({x,y})===0)return true;return false;});
 const mask=()=>page.evaluate(()=>{let t=worldManager.getActive().orangeTrees[0],a=sceneEditing.orangeTreeAnchor(t),result='';for(let y=a.y-58;y<a.y-4;y++)for(let x=a.x-32;x<a.x+32;x++)result+=sceneEditing.orangeTreeAt({x,y})===0?'1':'0';return result;});
 const before=await page.evaluate(()=>localStorage.getItem('gtlauncher.worlds.v1'));const first=await mask();await page.waitForTimeout(900);const second=await mask();
 if(reduced==='reduce')assert.equal(first,second,'reduced motion freezes the canopy');else assert.notEqual(first,second,'animated canopy visibly changes and hit test follows it');
 assert.equal(await page.evaluate(()=>localStorage.getItem('gtlauncher.worlds.v1')),before,'animation does not write world data');await page.close();
 }
 console.log('Browser canopy motion, live hit testing, reduced motion and unchanged world: OK');
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
