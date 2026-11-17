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
    return {portrait:rect('.character-hud-portrait'),vitals:rect('.character-hud-vitals'),rows:[...document.querySelectorAll('.character-hud-stats > div')].map(e=>{const r=e.getBoundingClientRect(); const b=e.querySelector('.stat-bar').getBoundingClientRect();return {left:r.left,right:r.right,top:r.top,bottom:r.bottom,barWidth:b.width}}),buttons:['#world-button','#weather-cycle-button'].map(s=>[rect(s),rect(s+' svg')])};
   });
   assert(geometry.vitals.left>=geometry.portrait.right-0.5,`vitals must be right of portrait at ${width}`);
   assert.equal(geometry.rows.length,4);
   assert(Math.abs(geometry.vitals.top-geometry.portrait.top)<0.5 && Math.abs(geometry.vitals.bottom-geometry.portrait.bottom)<0.5,`status height matches portrait at ${width}`);
   assert.equal(await page.locator('.stat-label').count(),0);
   for (let i=0;i<4;i++){assert(Math.abs(geometry.rows[i].barWidth-geometry.rows[0].barWidth)<0.5,`equal bar widths at ${width}`);if(i) assert(geometry.rows[i].top>=geometry.rows[i-1].bottom-0.5,`bars must stack at ${width}`);}
   for(const [button,icon] of geometry.buttons){
    assert(Math.abs((button.left+button.right-icon.left-icon.right)/2)<1,`icon horizontal center at ${width}`);
    assert(Math.abs((button.top+button.bottom-icon.top-icon.bottom)/2)<1,`icon vertical center at ${width}`);
    assert(icon.width<=button.width && icon.height<=button.height,`icon fits at ${width}`);
   }
  }
  console.log('Character HUD arrangement and toolbar icon centering: OK');
 } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1});
