// Real browser test: ordinary motion, a real touch and a mouse click on a 25x25 world.
const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try {
 const page=await browser.newPage({viewport:{width:1000,height:700},hasTouch:true,reducedMotion:'no-preference'}), errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'walk',worlds:[{id:'walk',name:'Walk',description:'',color:'#a8c795',gridSize:25,tiles:Array(625).fill(1),showCharacter:true,characterColumn:12,characterRow:12,characterScale:1,characterFacing:'S',house:null}]})));
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 await page.waitForFunction(()=>window.sceneEditing && window.worldManager && document.querySelector('.preview-stage').dataset.mode==='gameplay');
 const point=async(c,r)=>page.evaluate(({c,r})=>{let p=sceneEditing.centre({column:c,row:r}),cam=sceneEditing.camera(),rect=document.querySelector('#sky-canvas').getBoundingClientRect();return {x:rect.left+(160+(p.x-160)*.9+cam.panX)*rect.width/320,y:rect.top+(182+(p.y-182)*.9+cam.panY)*rect.height/200};},{c,r});
 const cdp=await page.context().newCDPSession(page);let target=await point(13,12);
 assert.equal(await page.evaluate(({x,y})=>document.elementFromPoint(x,y).id,target),'sky-canvas');
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...target,id:1}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
 await page.waitForFunction(()=>worldManager.getCharacter().column===13,null,{timeout:30000});
 target=await point(13,13);await page.mouse.click(target.x,target.y);
 await page.waitForFunction(()=>worldManager.getCharacter().row===13,null,{timeout:30000});
 assert.deepEqual(errors,[]); console.log('Real browser normal-motion touch and mouse movement, saved arrival on 25x25: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
