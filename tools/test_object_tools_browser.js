const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const b=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
const p=await b.newPage({viewport:{width:1000,height:900},reducedMotion:'reduce'});
await p.addInitScript(()=>{if(!localStorage.getItem('gtlauncher.worlds.v1'))localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'objects',worlds:[{id:'objects',name:'Objects',description:'',color:'#a8c795',gridSize:50,tiles:Array(2500).fill(1),showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:{column:28,row:27,scale:1,facing:'S',visible:true},orangeTrees:[{column:26,row:25}]}]}))});
await p.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await p.locator('#mode-map-editor').click();await p.evaluate(()=>sceneEditing.setCamera(3,0,40));
const clickTile=async target=>{const xy=await p.evaluate(t=>{const r=document.querySelector('#sky-canvas').getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom,n=sceneEditing.centre(t);return{x:r.left+(n.x-o.x)*z*r.width/320,y:r.top+(n.y-o.y)*z*r.height/200}},target);await p.mouse.click(xy.x,xy.y)};
async function selectTree(){
 const xy=await p.evaluate(()=>{const t=worldManager.getActive().orangeTrees[0],z=sceneEditing.camera().renderZoom,r=document.querySelector('#sky-canvas').getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),c=sceneEditing.centre({column:t.column+.5,row:t.row+.5,blockHeight:sceneEditing.surfaceHeight(t.column,t.row)});for(let y=c.y-40;y<c.y-10;y++)for(let x=c.x-15;x<c.x+15;x++){let sx=r.left+(x-o.x)*z*r.width/320,sy=r.top+(y-o.y)*z*r.height/200;if(sceneEditing.orangeTreeAt({x,y})===0&&!sceneEditing.hit({x,y},worldManager.getCharacter())&&document.elementFromPoint(sx,sy)?.id==='sky-canvas')return {x:sx,y:sy}}});assert(xy);await p.mouse.click(xy.x,xy.y);
}
await p.locator('#object-tool-select').click();await selectTree();assert(await p.locator('#object-tool-move').isEnabled());
await p.locator('#rotate-right').click();assert.equal((await p.evaluate(()=>worldManager.getActive().orangeTrees[0])).facing,'W');
await p.locator('#object-tool-move').click();await clickTile({column:24,row:24});assert.equal((await p.evaluate(()=>worldManager.getActive().orangeTrees[0])).column,26,'invalid move is rejected');
await clickTile({column:23,row:27});assert.deepEqual(await p.evaluate(()=>worldManager.getActive().orangeTrees[0]),{column:23,row:27,facing:'W'});
p.once('dialog',d=>d.accept());await p.locator('#object-tool-delete').click();assert.equal(await p.evaluate(()=>worldManager.getActive().orangeTrees),undefined);
await p.locator('#object-undo').click();assert.equal((await p.evaluate(()=>worldManager.getActive().orangeTrees[0])).column,23);await p.locator('#object-undo').click();assert.equal((await p.evaluate(()=>worldManager.getActive().orangeTrees[0])).column,26);
await p.locator('#object-redo').click();assert.equal((await p.evaluate(()=>worldManager.getActive().orangeTrees[0])).column,23);
await p.reload({waitUntil:'domcontentloaded'});assert.deepEqual(await p.evaluate(()=>worldManager.getActive().orangeTrees[0]),{column:23,row:27,facing:'W'});
await p.locator('#mode-map-editor').click();await p.evaluate(()=>sceneEditing.setCamera(3,0,40));
await p.locator('#object-tool-select').click();
const housePoint=await p.evaluate(()=>{const h=worldManager.getHouse(),c=sceneEditing.centreHouse(h),r=document.querySelector('#sky-canvas').getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom;for(let y=c.y-40;y<c.y+8;y++)for(let x=c.x-30;x<c.x+30;x++){const sx=r.left+(x-o.x)*z*r.width/320,sy=r.top+(y-o.y)*z*r.height/200;if(sceneEditing.houseAt({x,y},h)&&!sceneEditing.hit({x,y},worldManager.getCharacter())&&sceneEditing.orangeTreeAt({x,y})===null&&document.elementFromPoint(sx,sy)?.id==='sky-canvas')return{x:sx,y:sy}}});assert(housePoint);await p.mouse.click(housePoint.x,housePoint.y);
const initial=await p.evaluate(()=>({house:worldManager.getHouse(),centre:sceneEditing.centreHouse(worldManager.getHouse())}));
for(const facing of ['W','N','E','S']) {
 await p.locator('#rotate-right').click();
 const current=await p.evaluate(()=>({house:worldManager.getHouse(),centre:sceneEditing.centreHouse(worldManager.getHouse())}));
 assert.equal(current.house.facing,facing);assert.deepEqual(current.centre,initial.centre,'rotation preserves house anchor');
 assert.equal(current.house.column,initial.house.column);assert.equal(current.house.row,initial.house.row);
}
await p.locator('#rotate-right').click();assert.equal((await p.evaluate(()=>worldManager.getHouse())).facing,'W');
await p.locator('#object-tool-move').click();await clickTile({column:29,row:30});assert.equal((await p.evaluate(()=>worldManager.getHouse())).row,30);
p.once('dialog',d=>d.accept());await p.locator('#object-tool-delete').click();assert.equal(await p.evaluate(()=>worldManager.getHouse()),null);await p.locator('#object-undo').click();assert.equal((await p.evaluate(()=>worldManager.getHouse())).row,30);
await p.locator('#world-button').click();await p.locator('button[data-action="export"][data-id="objects"]').click();
const payload=JSON.parse(await p.locator('#world-export-json').inputValue());assert.equal(payload.version,6);assert.equal(payload.world.orangeTrees[0].facing,'W');
payload.world.id='objects-copy';payload.world.name='Objects copy';
await p.locator('#world-import-file').setInputFiles({name:'copy.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(payload))});
await p.waitForFunction(()=>worldManager.getActive().id==='objects-copy');assert.equal((await p.evaluate(()=>worldManager.getActive().orangeTrees[0])).facing,'W');
await p.locator('#world-close').click();
for(const width of [1000,500,280]){await p.setViewportSize({width,height:700});let tablet=await p.locator('.device-frame').boundingBox();for(const id of ['object-tool-select','object-tool-move','object-undo','object-redo','object-tool-delete']){let x=await p.locator('#'+id).boundingBox();assert(x && x.y+x.height<=tablet.y && x.x>=tablet.x && x.x+x.width<=tablet.x+tablet.width,'top controls fit above tablet');}let rotation=await p.locator('#rotate-right').boundingBox();assert(rotation && rotation.x+rotation.width<=tablet.x,'rotation remains left');}
console.log('Object tools: tree selection, rotate, move, delete, undo/redo, persistence, responsive layout: OK');
}finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
