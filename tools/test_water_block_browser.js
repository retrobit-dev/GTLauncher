// Real pointer editing of translucent water, including a stack and persisted state.
const assert=require('node:assert/strict');
const {chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  await page.locator('#mode-map-editor').click();await page.locator('#world-button').click();await page.locator('#world-new').click();await page.locator('#world-name').fill('Uji block air');await page.locator('#world-character').check();await page.locator('#world-form button[type=submit]').click();await page.locator('#world-close').click();
  await page.evaluate(()=>sceneEditing.setCamera(3,0,40));
  const target=await page.evaluate(()=>{const c=worldManager.getCharacter();return {column:c.column+1,row:c.row}});
  const click=async()=>{const p=await page.evaluate(t=>{const canvas=document.querySelector('#sky-canvas'),r=canvas.getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom,n=sceneEditing.centre(t);return {x:r.left+(n.x-o.x)*z*r.width/320,y:r.top+(n.y-o.y)*z*r.height/200};},target);assert.equal(await page.evaluate(p=>document.elementFromPoint(p.x,p.y)?.id,p),'sky-canvas');await page.mouse.click(p.x,p.y)};
  const stack=()=>page.evaluate(t=>{const w=worldManager.getActive(),i=t.row*w.gridSize+t.column;return w.blockStacks?.[i] || [w.tiles[i]]},target);
  await page.locator('#terrain-material').selectOption('3');assert(await page.locator('#water-block-preview').isVisible());assert(!(await page.locator('#grass-block-preview').isVisible()));
  await page.locator('#terrain-add').click();await click();assert.deepEqual(await stack(),[1,3]);
  await page.locator('#object-undo').click();assert.deepEqual(await stack(),[1]);await page.locator('#object-redo').click();assert.deepEqual(await stack(),[1,3]);
  // Sample the lower portion of the water cap while changing only the submerged block.
  const pixels=()=>page.evaluate(async t=>{await new Promise(r=>requestAnimationFrame(r));let canvas=document.querySelector('#sky-canvas'),p=sceneEditing.centre(t),c=sceneEditing.camera(),r=canvas.getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top);let x=(p.x-o.x)*c.renderZoom/320*canvas.width,y=(p.y-o.y+3)*c.renderZoom/200*canvas.height;return [...canvas.getContext('2d').getImageData(Math.round(x),Math.round(y),1,1).data];},target);
  const grass=await pixels();
  await page.evaluate(t=>{const w=worldManager.getActive(),s=w.blockStacks.map(x=>x.slice());s[t.row*w.gridSize+t.column][0]=4;if(!worldManager.updateBlockStacks(w.id,s))throw Error('update failed');},target);
  const stone=await pixels();assert.notDeepEqual(stone,grass,'water pixels show the material underneath');
  await page.reload({waitUntil:'domcontentloaded'});assert.deepEqual(await stack(),[4,3]);
  console.log('Water thumbnail, transparent stacked rendering, editing, history and reload: OK');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
