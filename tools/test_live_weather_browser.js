const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
try {for(const fail of [false,true]) {
 const context=await browser.newContext({geolocation:{latitude:-6.71,longitude:111.35},permissions:['geolocation'],reducedMotion:'reduce'});
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api.open-meteo.com/v1/forecast?**',route=>fail?route.abort():route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({current:{weather_code:3,cloud_cover:90,precipitation:1,rain:1,wind_speed_10m:10,wind_direction_10m:90}})}));
 await page.route('**/archive-api.open-meteo.com/**',route=>route.abort());
 await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 const label=page.locator('#weather-mode-label');await label.waitFor();
 await page.waitForFunction(fail=>document.querySelector('#weather-mode-label').textContent.includes(fail?'data cuaca tidak tersedia':'hujan 1 mm'),fail);
 assert.match(await label.innerText(),/lokasi perangkat/);
 if(!fail)assert.equal(await page.evaluate(()=>previewHudWeatherCode()),61);
 assert.deepEqual(errors,[]);await context.close();
 }
 console.log('Live weather success, rain icon, location and failed request status: OK');
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
