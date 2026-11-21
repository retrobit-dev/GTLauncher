const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'no-preference'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{
  const tiles=Array(400).fill(1),blockStacks=tiles.map(()=>[2,2,2,2,1]),roads=Array.from({length:9},(_,i)=>({column:i+5,row:11,material:'dirt'}));
  localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'routing',worlds:[{id:'routing',name:'Routing',description:'',color:'#a8c795',gridSize:20,tiles,blockStacks,roads,showCharacter:true,characterColumn:5,characterRow:10,characterScale:1,characterFacing:'S',house:null}]}));localStorage.setItem('gtlauncher.bridge-redesign-reset.v1','done');
  let original;Object.defineProperty(window,'GameplayPath',{configurable:true,get(){return original},set(value){original={...value,find(...args){const result=value.find(...args);window.lastRoadRoute=result;return result;}};}});
 });
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load',timeout:90000});await page.waitForFunction(()=>window.sceneEditing&&window.GameplayPath);
 const clicked=await page.evaluate(()=>{const canvas=document.querySelector('#sky-canvas'),rect=canvas.getBoundingClientRect(),origin=sceneEditing.toLocal(rect.left,rect.top),z=sceneEditing.camera().renderZoom,p=sceneEditing.centre({column:13,row:10,blockHeight:5}),x=rect.left+(p.x-origin.x)*z*rect.width/320,y=rect.top+(p.y-origin.y)*z*rect.height/200;for(const type of ['pointerdown','pointerup'])canvas.dispatchEvent(new PointerEvent(type,{pointerId:1,pointerType:'mouse',button:0,clientX:x,clientY:y,bubbles:true}));return {x,y};});
 await page.waitForFunction(()=>Array.isArray(window.lastRoadRoute),null,{timeout:10000});const route=await page.evaluate(()=>window.lastRoadRoute);
 assert.deepEqual(route[0],{column:5,row:10});assert.deepEqual(route.at(-1),{column:13,row:10});assert(route.some(p=>p.row===11),'walking route uses the road');
 await page.waitForFunction(()=>worldManager.getCharacter().column===13 && worldManager.getCharacter().row===10,null,{timeout:16000});
 async function send(column,row){return page.evaluate(({column,row})=>{const canvas=document.querySelector('#sky-canvas'),rect=canvas.getBoundingClientRect(),origin=sceneEditing.toLocal(rect.left,rect.top),z=sceneEditing.camera().renderZoom,p=sceneEditing.centre({column,row,blockHeight:5}),x=rect.left+(p.x-origin.x)*z*rect.width/320,y=rect.top+(p.y-origin.y)*z*rect.height/200;for(const type of ['pointerdown','pointerup'])canvas.dispatchEvent(new PointerEvent(type,{pointerId:1,pointerType:'mouse',button:0,clientX:x,clientY:y,bubbles:true}));},{column,row});}
 await send(5,10);await page.waitForTimeout(200);await send(10,9);await page.waitForFunction(()=>worldManager.getCharacter().column===10 && worldManager.getCharacter().row===9,null,{timeout:16000});
 assert.deepEqual(errors,[]);console.log('Gameplay click follows road-preferred route and arrives: OK');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
