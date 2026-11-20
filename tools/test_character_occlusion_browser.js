const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.setDefaultTimeout(60000);page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'fade-test',worlds:[{id:'fade-test',name:'Fade',description:'',color:'#a8c795',gridSize:50,tiles:Array(2500).fill(1),showCharacter:true,characterColumn:20,characterRow:20,characterScale:1,characterFacing:'S',house:{column:24,row:24,scale:1,facing:'S',visible:true},orangeTrees:[{column:32,row:32}]}]}));});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});await page.locator('#mode-map-editor').click();
 const result=await page.evaluate(()=>{
   const house=worldManager.getHouse(),tree=worldManager.getActive().orangeTrees[0],base=worldManager.getCharacter();
   const seek=(kind)=>{for(let c=kind==='house'?20:28;c<=(kind==='house'?26:33);c+=.25)for(let r=kind==='house'?20:28;r<=(kind==='house'?26:33);r+=.25){const p={...base,column:c,row:r};const depth=kind==='house'?house.column+house.row+2:tree.column+tree.row;if(c+r>depth)continue;const hit=kind==='house'?q=>sceneEditing.houseAt(q,house):q=>sceneEditing.treeContains(tree,q);if(sceneEditing.coversCharacter(p,hit))return p;}return null};
   const hp=seek('house'),tp=seek('tree');const original=CanvasRenderingContext2D.prototype.drawImage;let records=[];
   CanvasRenderingContext2D.prototype.drawImage=function(img,...args){if(img instanceof HTMLImageElement && /house|orange/i.test(img.src))records.push({src:img.src,alpha:this.globalAlpha});return original.call(this,img,...args)};
   function draw(p,term){records=[];sceneEditing.update(p,false,'character');return records.filter(x=>x.src.includes(term)).map(x=>x.alpha)}
   const h=hp?draw(hp,'house'):[],t=tp?draw(tp,'orange'):[],normal=draw({...base,column:10,row:10},'house');CanvasRenderingContext2D.prototype.drawImage=original;sceneEditing.update(null,false);
   return {hp,tp,h,t,normal};
 });
 assert(result.hp&&result.tp,JSON.stringify(result));assert(result.h.includes(.5),JSON.stringify(result));assert(result.t.includes(.5),JSON.stringify(result));assert(result.normal.includes(1),JSON.stringify(result));assert.deepEqual(errors,[]);console.log('Foreground house and tree fade only while covering the character: OK');
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
