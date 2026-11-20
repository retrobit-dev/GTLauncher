const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.setDefaultTimeout(60000);page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'group',worlds:[{id:'group',name:'Group',description:'',color:'#a8c795',gridSize:20,tiles:Array(400).fill(1),showCharacter:true,characterColumn:4,characterRow:4,characterScale:1,characterFacing:'S',house:null,orangeTrees:[{column:9,row:10,kind:'wild-grass',size:1}],fences:[{column:10,row:10,edge:'S',material:'wood'}]}]})));
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});await page.evaluate(()=>{document.getElementById('mode-map-editor').click();sceneEditing.setCamera(3,0,40);});
 for(let q=0;q<4;q++){
   console.log(`orientation ${q}`);
   const data=await page.evaluate(()=>{const canvas=document.querySelector('#sky-canvas'),r=canvas.getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom,w=worldManager.getActive(),t=sceneEditing.orangeTreeAnchor(w.orangeTrees[0]),f=sceneEditing.fenceGeometry(w.fences[0]);const cv=p=>({x:r.left+(p.x-o.x)*z*r.width/320,y:r.top+(p.y-o.y)*z*r.height/200});return {a:cv(t),b:cv({x:(f.a.x+f.b.x)/2,y:(f.a.y+f.b.y)/2}),dest:cv(sceneEditing.centre({column:11,row:10}))};});
   await page.evaluate(()=>document.getElementById('object-tool-select').click());
   await page.evaluate(data=>{const canvas=document.getElementById('sky-canvas');function send(type,x,y,buttons){canvas.dispatchEvent(new PointerEvent(type,{bubbles:true,pointerId:1,pointerType:'mouse',button:0,buttons,clientX:x,clientY:y}));}const a={x:Math.min(data.a.x,data.b.x)-5,y:Math.min(data.a.y,data.b.y)-5},b={x:Math.max(data.a.x,data.b.x)+5,y:Math.max(data.a.y,data.b.y)+5};send('pointerdown',a.x,a.y,1);send('pointermove',b.x,b.y,1);send('pointerup',b.x,b.y,0);},data);
   assert.match(await page.locator('#edit-status').textContent(),/2 objek/,`group selection q${q}`);
   await page.evaluate(()=>document.getElementById('object-tool-move').click());await page.evaluate(p=>{const canvas=document.getElementById('sky-canvas');for(const type of ['pointerdown','pointerup'])canvas.dispatchEvent(new PointerEvent(type,{bubbles:true,pointerId:2,pointerType:'mouse',button:0,clientX:p.x,clientY:p.y}));},data.dest);
   assert.deepEqual(await page.evaluate(()=>[worldManager.getActive().orangeTrees[0].column,worldManager.getActive().fences[0].column]),[11,12],`group move q${q}`);
   await page.evaluate(()=>document.getElementById('object-undo').click());assert.deepEqual(await page.evaluate(()=>[worldManager.getActive().orangeTrees[0].column,worldManager.getActive().fences[0].column]),[9,10],`undo q${q}`);
   await page.evaluate(()=>sceneEditing.turnIsland(1));
 }
 assert.deepEqual(errors,[]);console.log('Four orientations: drag selection of tree/fence group, move to world tile and undo: OK');await page.close();
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
