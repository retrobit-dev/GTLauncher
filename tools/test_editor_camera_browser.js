// Run with NODE_PATH containing playwright-core and CHROMIUM_PATH pointing to Chromium.
// A preview server must be running at PREVIEW_URL (defaults to localhost:4173).
const assert = require('node:assert/strict');
const {chromium} = require('playwright-core');
(async () => {
  const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH, args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
  try {
    const page = await browser.newPage({viewport:{width:800,height:600},hasTouch:true,reducedMotion:'reduce'});
    const errors=[]; page.on('pageerror', error=>errors.push(error.message));
    await page.goto(process.env.PREVIEW_URL || 'http://127.0.0.1:4173/', {waitUntil:'domcontentloaded'});
    await page.evaluate(() => document.fonts.ready);
    for (const width of [800, 400, 280]) {
      await page.setViewportSize({width,height:700});
      const placement=await page.evaluate(()=>{const tabs=document.querySelector('.mode-tabs').getBoundingClientRect(),world=document.getElementById('world-button').getBoundingClientRect(),bar=document.querySelector('.world-topbar').getBoundingClientRect();return {tabsRight:tabs.right,worldLeft:world.left,worldRight:world.right,barRight:bar.right,tabsY:(tabs.top+tabs.bottom)/2,worldY:(world.top+world.bottom)/2};});
      assert(placement.tabsRight <= placement.worldLeft && Math.abs(placement.tabsY-placement.worldY)<1 && placement.worldRight<=placement.barRight+1,'mode tabs and World share one row');
    }
    await page.setViewportSize({width:800,height:600});
    const hud = await page.evaluate(() => {
      const rect = selector => { const r = document.querySelector(selector).getBoundingClientRect(); return {top:r.top,bottom:r.bottom,height:r.height}; };
      const background = selector => getComputedStyle(document.querySelector(selector)).backgroundColor;
      return {left:rect('.character-hud'),right:rect('.clock-hud'),calendar:rect('.hud-calendar'),
        readouts:rect('.clock-readouts'),month:rect('#hud-month'),day:rect('#hud-day'),weekday:rect('#hud-weekday'),
        portraitBackground:background('.character-hud-portrait'),statsBackground:background('.character-hud-stats'),clockBackground:background('.clock-readouts')};
    });
    assert(Math.abs(hud.left.top-hud.right.top)<1 && Math.abs(hud.left.height-hud.right.height)<1, 'HUDs have matching overall heights');
    assert.equal(hud.portraitBackground,hud.clockBackground);assert.equal(hud.statsBackground,hud.clockBackground);
    assert(Math.abs(hud.calendar.bottom-hud.readouts.bottom)<1);
    assert(hud.month.bottom<hud.day.top && hud.day.bottom<hud.weekday.top && hud.weekday.bottom<hud.calendar.bottom, 'calendar text remains spaced and within panel');
    await page.locator('#mode-map-editor').click();
    const side = await page.evaluate(()=>{const frame=document.querySelector('.device-frame').getBoundingClientRect(),tools=document.querySelector('.terrain-tools').getBoundingClientRect();return {frameRight:frame.right,toolsLeft:tools.left};});
    assert(side.toolsLeft>side.frameRight,'tile editor is outside on the right');
    const layout=await page.evaluate(()=>{
      const rect=s=>document.querySelector(s).getBoundingClientRect(), frame=rect('.device-frame'), label=rect('#weather-mode-label'), object=rect('#object-library-button'), tile=rect('#terrain-draw');
      const iconSelectors=['#world-button','#weather-cycle-button','#object-library-button','#terrain-draw','#terrain-erase','#terrain-pick','#terrain-fill','#editor-pan','#editor-fit',...Array.from(document.querySelectorAll('.home-dock button')).map((_,i)=>`.home-dock button:nth-child(${i+1})`)];
      const errors=iconSelectors.map(s=>{const button=document.querySelector(s), icon=button.querySelector('svg,img'), b=button.getBoundingClientRect(),i=icon.getBoundingClientRect();return {s,dx:(i.left+i.right-b.left-b.right)/2,dy:(i.top+i.bottom-b.top-b.bottom)/2};}).filter(x=>Math.abs(x.dx)>1 || Math.abs(x.dy)>1);
      return {frameWidth:frame.width,labelWidth:label.width,objectBottom:object.bottom,tileTop:tile.top,errors};
    });
    assert(layout.objectBottom<=layout.tileTop,'object catalog sits above tile tools');
    assert(Math.abs(layout.labelWidth-layout.frameWidth)<1,'weather label spans tablet width');
    assert.deepEqual(layout.errors,[],'visible button icons are centered');
    await page.locator('#object-library-button').click();assert(await page.locator('#object-library-dialog').evaluate(e=>e.open),'object catalog opens');await page.locator('#object-library-close').click();

    await page.locator('#editor-pan').click();
    const overlay=page.locator('#editor-pan-surface'), box=await overlay.boundingBox();
    assert(box && box.width>0 && box.height>0);
    const x=box.x+box.width/2, y=box.y+box.height/2;
    const original=await page.evaluate(()=>({camera:sceneEditing.camera(), world:localStorage.getItem('gtlauncher.worlds.v1')}));
    const before=await page.screenshot();
    const cdp=await page.context().newCDPSession(page);
    await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y,id:1}]});
    await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:x+100,y:y+50,id:1}]});
    await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
    const changed=await page.evaluate(()=>({camera:sceneEditing.camera(), world:localStorage.getItem('gtlauncher.worlds.v1')}));
    assert(Math.abs(changed.camera.panX-original.camera.panX-100*320/box.width)<1);
    assert(Math.abs(changed.camera.panY-original.camera.panY-50*200/box.height)<1);
    assert.equal(changed.world,original.world,'camera drag must not write world data');
    const after=await page.screenshot();
    assert.notEqual(Buffer.compare(before,after),0,'rendered screen changes when island moves');
    assert.deepEqual(errors,[]);
    await page.locator('#editor-pan').click();
    await page.locator('#world-button').click();await page.locator('#world-new').click();
    await page.locator('#world-name').fill('Uji terrain');await page.locator('#world-character').check();await page.locator('#world-form button[type=submit]').click();
    await page.locator('#world-close').click();
    await page.locator('#terrain-material').selectOption('4');await page.locator('#terrain-draw').click();
    const target=await page.evaluate(()=>{const tile=sceneEditing.centre({column:4,row:4}),c=sceneEditing.camera(),r=document.getElementById('sky-canvas').getBoundingClientRect();return {x:r.left+(160+(tile.x-160)*.36*c.zoom+c.panX)*r.width/320,y:r.top+(182+(tile.y-182)*.36*c.zoom+c.panY)*r.height/200};});
    await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...target,id:2}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
    assert.equal(await page.evaluate(()=>worldManager.getActive().tiles[104]),4,'touch paints stone on E5');
    await page.locator('#mode-gameplay').click();
    const destination=await page.evaluate(()=>{const tile=sceneEditing.centre({column:13,row:12}),c=sceneEditing.camera(),r=document.getElementById('sky-canvas').getBoundingClientRect();return {x:r.left+(160+(tile.x-160)*c.renderZoom+c.panX)*r.width/320,y:r.top+(182+(tile.y-182)*c.renderZoom+c.panY)*r.height/200};});
    await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...destination,id:3}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
    assert.equal(await page.evaluate(()=>worldManager.getCharacter().column),13,'gameplay touch moves character');
    assert.deepEqual(errors,[]);
    console.log('Chromium touch pan, visible repaint, terrain tap and gameplay movement: OK');
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
