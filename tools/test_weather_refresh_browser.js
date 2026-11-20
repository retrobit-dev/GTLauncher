const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const b=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const ctx=await b.newContext({geolocation:{latitude:-6.71,longitude:111.35},permissions:['geolocation'],reducedMotion:'reduce'}),p=await ctx.newPage(),errors=[];p.on('pageerror',e=>errors.push(e.message));let n=0;
 await p.addInitScript(()=>{window.drizzleCount=0;const f=CanvasRenderingContext2D.prototype.fillRect;CanvasRenderingContext2D.prototype.fillRect=function(...args){if(this.fillStyle==='#c5d9db')window.drizzleCount++;return f.apply(this,args)}});
 await p.route('**/archive-api.open-meteo.com/**',r=>r.abort());
 await p.route('**/api.open-meteo.com/v1/forecast?**',r=>{n++;if(n===3)return r.abort();return r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({timezone:'Asia/Jakarta',current:{time:'2026-11-07T12:15',weather_code:n===1?3:51,cloud_cover:90,precipitation:n===1?0:.1,rain:n===1?0:.1,wind_speed_10m:10,wind_direction_10m:90}})});});
 await p.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
 const label=p.locator('#weather-mode-label'),refresh=p.locator('#weather-refresh-button');await p.waitForFunction(()=>document.querySelector('#weather-mode-label').textContent.includes('kode 3'));assert.match(await label.innerText(),/waktu model 2026-11-07 12:15 Asia\/Jakarta/);
 await refresh.click();await p.waitForFunction(()=>document.querySelector('#weather-mode-label').textContent.includes('kode 51'));await p.waitForFunction(()=>window.drizzleCount>0);assert.equal(await p.evaluate(()=>previewHudWeatherCode()),61);
 await refresh.click();await p.waitForFunction(()=>document.querySelector('#weather-mode-label').textContent.includes('pembaruan gagal'));assert.match(await label.innerText(),/kode 51/);assert.deepEqual(errors,[]);await ctx.close();console.log('Weather refresh, model timestamp, drizzle rendering and failed refresh retaining last snapshot: OK');
}finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
