const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{window.starDraws=[];const fill=CanvasRenderingContext2D.prototype.fillRect;CanvasRenderingContext2D.prototype.fillRect=function(x,y,w,h){if(String(this.fillStyle).startsWith('rgba(244, 245, 220,')){const t=this.getTransform();window.starDraws.push({x,y,w,h,scale:t.a});}return fill.call(this,x,y,w,h)}});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.route('**/archive-api.open-meteo.com/**',r=>r.abort());
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 for(let i=0;i<5;i++)await page.locator('#weather-cycle-button').click();
 await page.waitForFunction(()=>window.starDraws.length>=16);
 let stars=await page.evaluate(()=>window.starDraws.slice(-16));assert.equal(stars.length,16);assert.ok(stars.every(s=>s.scale>1));
 assert.deepEqual(errors,[]);console.log('Preview night stars draw with island zoom; no runtime errors: OK');
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
