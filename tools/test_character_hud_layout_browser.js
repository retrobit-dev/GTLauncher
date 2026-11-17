// Verify the HUD arrangement and toolbar icon centering at desktop and compact widths.
const assert = require('node:assert/strict');
const {chromium} = require('playwright-core');
(async () => {
 const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page = await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  for(const width of [1000,800,400,280]) {
   await page.setViewportSize({width,height:700});
   const geometry=await page.evaluate(()=>{
    const rect=s=>{const r=document.querySelector(s).getBoundingClientRect();return {left:r.left,right:r.right,top:r.top,bottom:r.bottom,width:r.width,height:r.height}};
    return {portrait:rect('.character-hud-portrait'),vitals:rect('.character-hud-vitals'),needs:rect('.character-hud-needs'),buttons:['#world-button','#weather-cycle-button'].map(s=>[rect(s),rect(s+' svg')])};
   });
   assert(geometry.vitals.left>=geometry.portrait.right-0.5,`vitals must be right of portrait at ${width}`);
   assert(geometry.needs.top>=geometry.portrait.bottom-0.5,`needs must be below portrait at ${width}`);
   for(const [button,icon] of geometry.buttons){
    assert(Math.abs((button.left+button.right-icon.left-icon.right)/2)<1,`icon horizontal center at ${width}`);
    assert(Math.abs((button.top+button.bottom-icon.top-icon.bottom)/2)<1,`icon vertical center at ${width}`);
    assert(icon.width<=button.width && icon.height<=button.height,`icon fits at ${width}`);
   }
  }
  console.log('Character HUD arrangement and toolbar icon centering: OK');
 } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1});
