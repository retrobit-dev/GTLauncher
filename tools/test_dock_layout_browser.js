// Chromium layout regression for the preview dock, including small preview sizes.
const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'});
  await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
  for(const width of [1000,800,400,280]) {
   await page.setViewportSize({width,height:700});
   const result=await page.evaluate(()=>{
    const dock=document.querySelector('.home-dock'), d=dock.getBoundingClientRect(), style=getComputedStyle(dock);
    const inset=side=>parseFloat(style['border'+side+'Width'])+parseFloat(style['padding'+side]);
    const inner={left:d.left+inset('Left'),right:d.right-inset('Right'),top:d.top+inset('Top'),bottom:d.bottom-inset('Bottom')};
    const rect=r=>({left:r.left,right:r.right,top:r.top,bottom:r.bottom});
    return {inner,buttons:[...dock.querySelectorAll('button')].map(b=>({button:rect(b.getBoundingClientRect()),icon:rect(b.querySelector('img').getBoundingClientRect())}))};
   });
   assert.equal(result.buttons.length,11);
   const within=(r,p)=>r.left>=p.left-0.2 && r.right<=p.right+0.2 && r.top>=p.top-0.2 && r.bottom<=p.bottom+0.2;
   for(const {button,icon} of result.buttons){assert(within(button,result.inner),`slot background overflows dock at ${width}px`);assert(within(icon,button),`icon overflows slot at ${width}px`);}
  }
  await page.setViewportSize({width:1000,height:700});
  await page.locator('.home-dock [data-dock="hand"]').click();
  assert(await page.locator('#dock-dialog').evaluate(e=>e.open),'dock button still opens its dialog');
  console.log('Dock backgrounds and icons fit inside the dock at all tested widths; button works: OK');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
