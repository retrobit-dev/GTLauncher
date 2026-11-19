const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{let tiles=Array(400).fill(0);tiles[210]=1;localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'r',worlds:[{id:'r',name:'Ramp',description:'',color:'#a8c795',gridSize:20,tiles,showCharacter:false,characterColumn:10,characterRow:10,characterScale:1,characterFacing:'S',house:null}]}));});await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});
 const report=await page.evaluate(()=>{const world=worldManager.getActive(),canvas=document.getElementById('sky-canvas'),ctx=canvas.getContext('2d'),result=[];
   function pixels(){sceneEditing.setGameplayDraft(null);return ctx.getImageData(0,0,canvas.width,canvas.height).data;}
   for(let material=1;material<=10;material++){
    world.tiles[210]=material;delete world.ramps;const baseline=pixels();
    for(const direction of ['N','E','S','W']) {world.ramps=[{column:10,row:10,direction}];const image=pixels();let changed=0;for(let i=0;i<image.length;i+=4)if(image[i]!==baseline[i]||image[i+1]!==baseline[i+1]||image[i+2]!==baseline[i+2])changed++;result.push({material,direction,changed});}
   }delete world.ramps;return result;});
 for(const entry of report)assert(entry.changed>20,JSON.stringify(entry));assert.deepEqual(errors,[]);console.log('Ramp preview visibly renders four orientations for all ten materials: OK');await page.close();
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
