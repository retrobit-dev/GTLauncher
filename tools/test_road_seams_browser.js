const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const stacks=Array.from({length:400},()=>[2,2,2,2,1]);stacks[10*20+12].push(1);const w={id:'seams',name:'Seams',description:'',color:'#a8c795',gridSize:20,tiles:Array(400).fill(1),blockStacks:stacks,showCharacter:true,characterColumn:5,characterRow:5,characterScale:1,characterFacing:'S',house:null};localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'seams',worlds:[w]}));localStorage.setItem('gtlauncher.bridge-redesign-reset.v1','done');
  window.seamStrokes=[];const stroke=CanvasRenderingContext2D.prototype.stroke;CanvasRenderingContext2D.prototype.stroke=function(...args){if(Math.abs(this.lineWidth-.8)<.001 && ['#a66d47','#468fb7'].includes(this.strokeStyle))window.seamStrokes.push(this.strokeStyle);return stroke.apply(this,args);};});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load',timeout:90000});await page.locator('#mode-map-editor').click();await page.evaluate(()=>sceneEditing.setCamera(3,0,40));
 const put=async(roads)=>page.evaluate(roads=>{const w=worldManager.getActive();if(!worldManager.replaceRoads(w.id,w.roads||[],roads))throw new Error('roads rejected');window.seamStrokes=[];sceneEditing.setGameplayDraft(null);return window.seamStrokes;},roads);
 const first=[{column:10,row:10,material:'dirt'},{column:11,row:10,material:'dirt'},{column:12,row:10,material:'dirt'},{column:10,row:11,material:'water'}];
 assert.deepEqual(await put(first),['#a66d47'],'only same material and height gets a seam bridge');
 const water=[{column:10,row:10,material:'water'},{column:11,row:10,material:'water'}];assert.deepEqual(await put(water),['#468fb7']);
 await page.evaluate(()=>sceneEditing.turnIsland(1));assert.deepEqual(await put(water),['#468fb7'],'connections follow camera rotation');
 assert.deepEqual(errors,[]);console.log('Same-height road and water seams, material/height isolation, view rotation: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
