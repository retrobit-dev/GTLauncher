const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:process.env.ANIMATED?'no-preference':'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.locator('#mode-map-editor').click();await page.locator('#world-button').click();await page.locator('#world-new').click();await page.locator('#world-name').fill('Uji kolam');await page.locator('#world-character').check();await page.locator('#world-form button[type=submit]').click();await page.locator('#world-close').click();
  const target=await page.evaluate(()=>{const c=worldManager.getCharacter();return {column:c.column+1,row:c.row}});
  await page.evaluate(p=>{const w=worldManager.getActive(),t=w.tiles.slice();for(let r=0;r<2;r++)for(let c=0;c<2;c++)t[(p.row+r)*w.gridSize+p.column+c]=3;if(!worldManager.updateTiles(w.id,t))throw Error('cannot save')},target);
  const layout=()=>page.evaluate(()=>{const data=PondLife.build(worldManager.getActive());return data.ponds.map(({area,fish,lilypads})=>({area,fish,lilypads}));});
  assert.deepEqual(await layout(),[{area:4,fish:2,lilypads:1}]);
  await page.evaluate(p=>{sceneEditing.setCamera(5,0,0);const c=sceneEditing.centre(p),z=sceneEditing.camera().renderZoom;sceneEditing.setCamera(5,-(c.x-160)*z,100-182-(c.y-182)*z)},target);
  const before=await page.evaluate(()=>localStorage.getItem('gtlauncher.worlds.v1'));
  if(process.env.SAVE_SCREENSHOT)await page.screenshot({path:'pond-inspect.png'});
  if(process.env.ANIMATED){
    const box=await page.locator('#sky-canvas').boundingBox();const clip={x:Math.round(box.x+box.width*.46),y:Math.round(box.y+box.height*.45),width:55,height:45};
    const a=await page.screenshot({clip});await page.waitForTimeout(800);const b=await page.screenshot({clip});assert(!a.equals(b),'pond decorations animate');
  }
  assert.equal(await page.evaluate(()=>localStorage.getItem('gtlauncher.worlds.v1')),before,'animation never writes world data');
  await page.locator('#mode-gameplay').click();assert.deepEqual(await layout(),[{area:4,fish:2,lilypads:1}]);
  await page.reload({waitUntil:'domcontentloaded'});assert.deepEqual(await layout(),[{area:4,fish:2,lilypads:1}]);
  console.log('Pond life in editor, gameplay, animation and reload: OK');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
