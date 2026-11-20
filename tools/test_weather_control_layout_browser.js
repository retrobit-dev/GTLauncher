const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage']});
 try {
  for(const width of [360,1200]){
   const page=await browser.newPage({viewport:{width,height:800},reducedMotion:'reduce'}),errors=[];
   page.on('pageerror',e=>errors.push(e.message));
   await page.route('**/api.open-meteo.com/**',r=>r.abort());
   await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded',timeout:90000});
   await page.waitForFunction(()=>window.sceneEditing);
   const result=await page.evaluate(()=>{
    const rect=id=>document.querySelector(id).getBoundingClientRect().toJSON();
    const label=rect('#weather-mode-label'),frame=rect('.device-frame'),row=rect('.preview-controls'),button=rect('#weather-cycle-button'),refresh=rect('#weather-refresh-button');
    return {label,frame,row,button,refresh,wrap:getComputedStyle(document.querySelector('.preview-controls')).flexWrap,sliderHidden:getComputedStyle(document.querySelector('#house-transparency-control')).display==='none'};
   });
   assert(Math.abs(result.label.width-result.frame.width)<1);
   assert(result.label.top>=result.row.bottom);
   assert(Math.abs(result.button.top-result.refresh.top)<2);
   assert.equal(result.wrap,'nowrap');assert(result.sliderHidden);assert.deepEqual(errors,[]);
   await page.close();
  }
  console.log('Weather label full tablet width below one button row, desktop and narrow: OK');
 } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1});
