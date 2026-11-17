const assert=require('node:assert/strict');
const {chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:process.env.ANIMATED?'no-preference':'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  await page.locator('#mode-map-editor').click();await page.locator('#world-button').click();await page.locator('#world-new').click();await page.locator('#world-name').fill('Uji karakter air');await page.locator('#world-character').check();await page.locator('#world-form button[type=submit]').click();await page.locator('#world-close').click();
  const start=await page.evaluate(()=>{const c=worldManager.getCharacter();return {column:c.column,row:c.row}}),water={column:start.column+1,row:start.row};
  await page.evaluate(p=>{const w=worldManager.getActive(),tiles=w.tiles.slice();tiles[p.row*w.gridSize+p.column]=3;if(!worldManager.updateTiles(w.id,tiles))throw Error('cannot set water')},water);
  assert.equal(await page.evaluate(p=>sceneEditing.waterDepth(p.column,p.row),water),8);assert.equal(await page.evaluate(p=>sceneEditing.waterDepth(p.column,p.row),start),0);
  await page.locator('#mode-gameplay').click();
  const click=async p=>{const q=await page.evaluate(p=>{const canvas=document.querySelector('#sky-canvas'),r=canvas.getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom,n=sceneEditing.centre(p);return {x:r.left+(n.x-o.x)*z*r.width/320,y:r.top+(n.y-o.y)*z*r.height/200};},p);await page.mouse.click(q.x,q.y)};
  await click(water);await page.waitForFunction(p=>{const c=worldManager.getCharacter();return c.column===p.column&&c.row===p.row},water,{timeout:10000});assert.deepEqual(await page.evaluate(()=>{const c=worldManager.getCharacter();return {column:c.column,row:c.row}}),water,'character enters water');
  const hit=await page.evaluate(()=>{const c=worldManager.getCharacter(),p=sceneEditing.centre(c);return sceneEditing.hit({x:p.x,y:p.y+6},c)});assert(hit,'character hit area follows submerged position');
  await click(start);await page.waitForFunction(p=>{const c=worldManager.getCharacter();return c.column===p.column&&c.row===p.row},start,{timeout:10000});assert.deepEqual(await page.evaluate(()=>{const c=worldManager.getCharacter();return {column:c.column,row:c.row}}),start,'character exits water');
  console.log('Character enters, stands in, and leaves water; hit area follows: OK');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
