// End-to-end: edit a stone block by real pointer, switch to gameplay, and reload.
const assert = require('node:assert/strict');
const {chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  await page.locator('#mode-map-editor').click();
  await page.locator('#world-button').click();await page.locator('#world-new').click();
  await page.locator('#world-name').fill('Uji block batu');await page.locator('#world-character').check();
  await page.locator('#world-form button[type=submit]').click();await page.locator('#world-close').click();
  assert.equal(await page.evaluate(()=>worldManager.getActive().gridSize),50);
  await page.evaluate(()=>sceneEditing.setCamera(3,0,0));
  const target=await page.evaluate(()=>{
   const canvas=document.querySelector('#sky-canvas'), rect=canvas.getBoundingClientRect(), origin=sceneEditing.toLocal(rect.left,rect.top), zoom=sceneEditing.camera().renderZoom;
   const world=worldManager.getActive(), ch=worldManager.getCharacter(), house=worldManager.getHouse();
   for(let r=18;r<31;r++)for(let c=18;c<31;c++){
    if(Math.abs(c-ch.column)+Math.abs(r-ch.row)<3)continue;
    if(house?.visible && c>=house.column && c<house.column+3 && r>=house.row && r<house.row+3)continue;
    const n=sceneEditing.centre({column:c,row:r});
    const x=rect.left+(n.x-origin.x)*zoom*rect.width/320, y=rect.top+(n.y-origin.y)*zoom*rect.height/200;
    if(x<rect.left+25 || x>rect.right-25 || y<rect.top+25 || y>rect.bottom-25 || document.elementFromPoint(x,y)!==canvas)continue;
    return {column:c,row:r,x,y,index:r*world.gridSize+c};
   }
   return null;
  });
  assert(target,'a visible, unoccupied grid cell is available');
  const value=()=>page.evaluate(i=>worldManager.getActive().tiles[i],target.index);
  const paint=async(name,material)=>{if(material!==undefined){await page.locator('#block-picker-trigger').click();await page.locator(`[data-block-value=\"${String(material)}\"]`).click();}await page.locator('#terrain-'+name).click();await page.mouse.click(target.x,target.y);};
  await paint('draw',4);assert.equal(await value(),4,'replace grass with stone');
  assert(await page.locator('#stone-block-preview').isVisible(),'selected stone concept is shown');
  assert.equal(await page.locator('#grass-block-preview').isVisible(),false,'grass thumbnail hides');
  assert.match(await page.locator('#terrain-status').textContent(),/block diganti/);
  await paint('erase');assert.equal(await value(),0,'erase creates an empty grid cell');
  await paint('add',4);assert.equal(await value(),4,'add stone to empty grid cell');
  assert.match(await page.locator('#terrain-status').textContent(),/block ditambahkan/);
  await page.locator('#mode-gameplay').click();assert.equal(await value(),4,'gameplay reads the stone block');
  assert(await page.evaluate(({column,row})=>{const w=worldManager.getActive(),p={column,row};return GameplayPath.find(w,p,p,null)!==null;},target),'stone is walkable');
  await page.reload({waitUntil:'domcontentloaded'});assert.equal(await value(),4,'stone survives reload without becoming farmland');
  console.log('Stone replacement, addition, gameplay and reload: OK');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
