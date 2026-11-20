const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.setContent(`<div class="preview-stage" data-mode="gameplay"><canvas id="sky-canvas"></canvas><p id="gameplay-movement-status"></p></div><div class="home-dock"></div><button id="mode-map-editor"></button>`);
 await page.evaluate(()=>{let c={visible:true,column:2,row:2,scale:1,facing:'E'},stacks=Array.from({length:400},()=>[]);stacks[42]=[1,1];stacks[43]=[1,1];stacks[44]=[1,1,1];const world={id:'test',gridSize:20,blockStacks:stacks,tiles:stacks.map(s=>s.length?1:0),ramps:[{column:3,row:2,direction:'E'}]};window.worldManager={getActive:()=>world,getHouse:()=>null,getCharacter:()=>({...c}),updateCharacter:(id,v)=>(c={...v},true)};window.sceneEditing={setGameplayDraft:d=>window.draft=d,setGameplayRoute(){},toLocal:(x,y)=>({x,y}),tileAt:p=>({column:p.x,row:p.y}),hit:()=>false,houseAt:()=>false,surfaceHeight:(x,y)=>stacks[y*20+x].length,waterDepth:()=>0};window.matchMedia=()=>({matches:false});window.requestAnimationFrame=fn=>(window.raf=fn,1);});
 for(const path of ['preview/bridge-geometry.js','preview/ramp-geometry.js','preview/gameplay-path.js','preview/jump-motion.js','preview/gameplay-movement.js'])await page.addScriptTag({path});
 async function tap(x,y){await page.evaluate(([x,y])=>{const canvas=document.querySelector('#sky-canvas');for(const type of ['pointerdown','pointerup'])canvas.dispatchEvent(new PointerEvent(type,{pointerId:1,pointerType:'touch',clientX:x,clientY:y,bubbles:true}));},[x,y]);}
 await tap(4,2);await page.evaluate(()=>raf(1000));await page.evaluate(()=>raf(1162.5));let d=await page.evaluate(()=>draft);assert.equal(d.blockHeight,2);assert.equal(d.jumpHeight,0);
 await page.evaluate(()=>raf(1487.5));d=await page.evaluate(()=>draft);assert.equal(d.blockHeight,2.25);assert.equal(d.jumpHeight,0);
 await page.evaluate(()=>raf(1812.5));d=await page.evaluate(()=>draft);assert.equal(d.blockHeight,2.75);assert.equal(d.jumpHeight,0);
 await page.evaluate(()=>raf(2137.5));d=await page.evaluate(()=>draft);assert.equal(d.blockHeight,3);assert.equal(d.jumpHeight,0);
 await page.evaluate(()=>raf(2300));assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,4);
 await tap(2,2);await page.evaluate(()=>raf(3000));await page.evaluate(()=>raf(3487.5));d=await page.evaluate(()=>draft);assert.equal(d.blockHeight,2.75);assert.equal(d.jumpHeight,0);
 await page.evaluate(()=>raf(3812.5));d=await page.evaluate(()=>draft);assert.equal(d.blockHeight,2.25);assert.equal(d.jumpHeight,0);await page.evaluate(()=>raf(4300));assert.equal((await page.evaluate(()=>worldManager.getCharacter())).column,2);
 assert.deepEqual(errors,[]);console.log('Character follows ramp surface without jumping in both directions: OK');await page.close();
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
