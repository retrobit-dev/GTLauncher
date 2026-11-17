const assert=require('node:assert/strict');
const {chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:process.env.ANIMATED ? 'no-preference' : 'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  await page.locator('#mode-map-editor').click();await page.locator('#world-button').click();await page.locator('#world-new').click();await page.locator('#world-name').fill('Uji air terjun');await page.locator('#world-character').check();await page.locator('#world-form button[type=submit]').click();await page.locator('#world-close').click();
  await page.evaluate(()=>sceneEditing.setCamera(3,0,40));
  const t=await page.evaluate(()=>{let c=worldManager.getCharacter();return {column:c.column+1,row:c.row}});
  const set=async(c,r,s)=>page.evaluate(({c,r,s})=>{const w=worldManager.getActive(),a=w.blockStacks ? w.blockStacks.map(x=>x.slice()) : w.tiles.map(x=>x?[x]:[]);a[r*w.gridSize+c]=s;if(!worldManager.updateBlockStacks(w.id,a))throw Error('save failed')},{c,r,s});
  await set(t.column,t.row,[1,1,3]);await set(t.column+1,t.row,[3]);await set(t.column,t.row+1,[3]);await set(t.column-1,t.row,[3]);await set(t.column,t.row-1,[3]);
  assert.deepEqual(await page.evaluate(p=>sceneEditing.waterFalls(p.column,p.row),t),{left:2,right:2,backLeft:2,backRight:2},'all four downhill water neighbours produce falls');
  assert.deepEqual(await page.evaluate(p=>sceneEditing.waterTopBorders(p.column,p.row),t),{left:false,right:false,backLeft:false,backRight:false},'waterfall openings do not get blocked by the upper rim');
  assert.equal((await page.evaluate(p=>sceneEditing.waterTopBorders(p.column+1,p.row),t)).backLeft,false,'lower surface is open toward the upper water too');
  await set(t.column+1,t.row,[1,3]);assert.equal((await page.evaluate(p=>sceneEditing.waterFalls(p.column,p.row),t)).right,1,'drop follows actual height');
  await set(t.column+1,t.row,[1,1,3]);assert(!('right' in await page.evaluate(p=>sceneEditing.waterFalls(p.column,p.row),t)),'level water joins without a waterfall');
  await set(t.column+1,t.row,[1,1,1]);assert(!('right' in await page.evaluate(p=>sceneEditing.waterFalls(p.column,p.row),t)),'no fall onto non-water');
  await set(t.column+1,t.row,[3]);
  if(process.env.ANIMATED){
    const p=await page.evaluate(t=>{const canvas=document.querySelector('#sky-canvas'),r=canvas.getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom,n=sceneEditing.centre(t);return {x:r.left+(n.x-o.x)*z*r.width/320,y:r.top+(n.y-o.y)*z*r.height/200};},t);
    const clip={x:Math.round(p.x+2),y:Math.round(p.y+3),width:12,height:15};
    const first=await page.screenshot({clip});await page.waitForTimeout(650);const second=await page.screenshot({clip});
    assert(!first.equals(second),'flow highlights animate');
  }
  await page.reload({waitUntil:'domcontentloaded'});assert.deepEqual(await page.evaluate(p=>sceneEditing.waterFalls(p.column,p.row),t),{left:2,right:2,backLeft:2,backRight:2},'falls survive reload as derived display');
  console.log('Waterfall detection in four directions, level checks, and persistence: OK');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
