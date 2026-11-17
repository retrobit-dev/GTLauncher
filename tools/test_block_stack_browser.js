// Exercise stacking with real pointer events, history, gameplay and reload.
const assert=require('node:assert/strict');
const {chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:process.env.ANIMATED ? 'no-preference' : 'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  await page.locator('#mode-map-editor').click();await page.locator('#world-button').click();await page.locator('#world-new').click();
  await page.locator('#world-name').fill('Uji block bertingkat');await page.locator('#world-character').check();await page.locator('#world-form button[type=submit]').click();await page.locator('#world-close').click();
  await page.evaluate(()=>sceneEditing.setCamera(3,0,40));
  const target=await page.evaluate(()=>{let c=worldManager.getCharacter();return {column:c.column+1,row:c.row};});
  const pos=async p=>page.evaluate(p=>{const canvas=document.querySelector('#sky-canvas'),rect=canvas.getBoundingClientRect(),o=sceneEditing.toLocal(rect.left,rect.top),z=sceneEditing.camera().renderZoom,n=sceneEditing.centre(p);return {x:rect.left+(n.x-o.x)*z*rect.width/320,y:rect.top+(n.y-o.y)*z*rect.height/200};},p);
  const stack=()=>page.evaluate(({column,row})=>{let w=worldManager.getActive(),i=row*w.gridSize+column;return w.blockStacks?.[i] ?? (w.tiles[i]?[w.tiles[i]]:[]);},target);
  const clickTarget=async()=>{let p=await pos(target);assert.equal(await page.evaluate(({x,y})=>document.elementFromPoint(x,y)?.id,p),'sky-canvas');await page.mouse.click(p.x,p.y);};
  let ground=await page.evaluate(p=>sceneEditing.centre(p).y,target);
  await page.locator('#terrain-material').selectOption('2');await page.locator('#terrain-add').click();await clickTarget();
  assert.deepEqual(await stack(),[1,2]);assert.equal(await page.evaluate(p=>sceneEditing.centre(p).y,target),ground-8);
  assert.deepEqual(await page.evaluate(p=>sceneEditing.tileAt(sceneEditing.centre(p)),target),target,'raised top is selectable');
  await clickTarget();assert.deepEqual(await stack(),[1,2,2]);
  assert.equal(await page.evaluate(p=>GameplayPath.find(worldManager.getActive(),worldManager.getCharacter(),p,worldManager.getHouse()),target),null,'two-level cliff blocks access');
  await page.locator('#object-undo').click();assert.deepEqual(await stack(),[1,2]);
  await page.locator('#object-undo').click();assert.deepEqual(await stack(),[1]);await page.locator('#object-redo').click();assert.deepEqual(await stack(),[1,2]);
  await page.locator('#terrain-material').selectOption('4');await page.locator('#terrain-draw').click();await clickTarget();assert.deepEqual(await stack(),[1,4],'pencil changes top only');
  await page.locator('#terrain-erase').click();await clickTarget();assert.deepEqual(await stack(),[1],'erase removes top only');await page.locator('#object-undo').click();assert.deepEqual(await stack(),[1,4]);
  await page.locator('#mode-gameplay').click();
  const path=await page.evaluate(p=>GameplayPath.find(worldManager.getActive(),worldManager.getCharacter(),p,worldManager.getHouse()),target);assert(path && path.length===2,'one-step elevation reachable');
  await clickTarget();await page.waitForFunction(p=>{let c=worldManager.getCharacter();return c.column===p.column&&c.row===p.row},target,{timeout:10000});assert.deepEqual(await page.evaluate(()=>{const {column,row}=worldManager.getCharacter();return {column,row};}),target,'character reaches elevated block');
  assert.equal(await page.evaluate(()=>sceneEditing.centre(worldManager.getCharacter()).y),ground-8);
  await page.reload({waitUntil:'domcontentloaded'});assert.deepEqual(await stack(),[1,4],'stack survives reload');
  console.log('Browser stack editing, history, raised hit testing, gameplay and reload: OK');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
