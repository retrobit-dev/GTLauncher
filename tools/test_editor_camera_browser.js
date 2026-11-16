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
    await page.locator('#mode-map-editor').click();
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
    console.log('Chromium touch drag, visible repaint and world-data isolation: OK');
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
