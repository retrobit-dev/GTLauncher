const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('jsdom');
const root=path.join(__dirname,'../preview');
(async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://test.invalid/',runScripts:'outside-only'}),w=dom.window;
 const now=new Date('2026-11-07T12:00:00Z'),RealDate=w.Date;
 w.Date=class extends RealDate{constructor(...args){super(...(args.length?args:[now.getTime()]));}static now(){return now.getTime();}};
 let location={latitude:-6.7,longitude:110.4},interval,pending=[];
 w.previewHudLocation=()=>location;w.previewHudSunAltitude=()=>0;w.SeasonEstimate=require('../preview/season-estimate.js');w.setInterval=fn=>{interval=fn};
 w.fetch=()=>new Promise(resolve=>pending.push(resolve));
 let events=0;w.addEventListener('previewseasonchange',()=>events++);
 w.eval(fs.readFileSync(path.join(root,'hud.js'),'utf8'));
 assert.equal(w.previewSeason(),'unknown');
 const time=[],rain=[];for(let i=-100;i<=-7;i++){time.push(new Date(Date.UTC(2026,10,7)+i*86400000).toISOString().slice(0,10));rain.push(6);}
 pending.shift()({ok:true,json:async()=>({daily:{time,precipitation_sum:rain}})});
 await new Promise(resolve=>setImmediate(resolve));assert.equal(w.previewSeason(),'wet');assert.match(w.document.getElementById('hud-season').textContent,/hujan/);assert(events>0);
 location={latitude:-7.1,longitude:110.4};interval();assert.equal(w.previewSeason(),'unknown');
 const stale=pending.shift();location=null;interval();assert.equal(w.previewSeason(),'unknown');stale({ok:true,json:async()=>({daily:{time,precipitation_sum:rain}})});
 await new Promise(resolve=>setImmediate(resolve));assert.equal(w.previewSeason(),'unknown','stale responses cannot restore an old location season');
 dom.window.close();console.log('Gameplay season exposes validated wet/dry/unknown and rejects stale location results: OK');
})().catch(e=>{console.error(e);process.exitCode=1});
