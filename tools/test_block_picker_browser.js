const assert=require('node:assert/strict');
const {chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});await page.locator('#mode-map-editor').click();
  for(const width of [1000,800,500,420,400,280]) {
   await page.setViewportSize({width,height:700});
   const trigger=page.locator('#block-picker-trigger');await trigger.click();assert.equal(await trigger.getAttribute('aria-expanded'),'true');
   for(const [value,name] of [['1','Rumput'],['2','Tanah'],['3','Air'],['4','Batu'],['5','Pasir']]) {
    let option=page.locator(`[data-block-value="${value}"]`);await option.scrollIntoViewIfNeeded();
    assert(await option.isVisible());assert.equal(await option.locator('span').first().textContent(),name);
    const bounds=await option.evaluate(el=>{const r=el.getBoundingClientRect(),label=el.querySelector('span');return {left:r.left,right:r.right,labelFits:label.scrollWidth<=label.clientWidth}});
    assert(bounds.left>=0 && bounds.right<=width && bounds.labelFits,`${name} fits screen and its row at ${width}px`);
   }
   await page.locator('[data-block-value="5"]').click();assert.equal(await page.locator('#terrain-material').inputValue(),'5');assert.equal(await page.locator('#block-picker-value').textContent(),'Pasir');assert(await page.locator('#sand-block-preview').isVisible());
   assert.equal(await trigger.getAttribute('aria-expanded'),'false');
   await trigger.click();await page.keyboard.press('Home');await page.keyboard.press('ArrowDown');await page.keyboard.press('Enter');
   assert.equal(await page.locator('#terrain-material').inputValue(),'2','keyboard selection updates the editor material');
   await trigger.click();await page.keyboard.press('Escape');assert.equal(await trigger.getAttribute('aria-expanded'),'false');assert(await trigger.evaluate(el=>document.activeElement===el));
   const bounds=await page.evaluate(()=>{let a=document.querySelector('.device-frame').getBoundingClientRect(),b=document.querySelector('.editor-right-controls').getBoundingClientRect(),t=document.querySelector('#block-picker-trigger').getBoundingClientRect();return {tabletRight:a.right,panelLeft:b.left,panelRight:b.right,labelFits:document.querySelector('#block-picker-value').scrollWidth<=document.querySelector('#block-picker-value').clientWidth,triggerWidth:t.width}});
   assert(bounds.panelLeft>=bounds.tabletRight && bounds.panelRight<=width && bounds.labelFits,`picker stays outside tablet and text fits at ${width}px`);
  }
  await page.locator('#mode-gameplay').click();assert(!(await page.locator('#block-picker-trigger').isVisible()));
  console.log('Block picker layout, selection, keyboard, and responsive scrolling: OK');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
