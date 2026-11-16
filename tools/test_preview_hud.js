const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {JSDOM}=require('jsdom'),season=require('../preview/season-estimate.js');
const root=path.join(__dirname,'../preview');
function rainfall(mm) {const today=Date.UTC(2026,10,7), dates=[],amount=[];for(let i=-100;i<=-7;i++){dates.push(new Date(today+i*86400000).toISOString().slice(0,10));amount.push(mm);}return {daily:{time:dates,precipitation_sum:amount}};}
const now=new Date('2026-11-07T12:00:00Z');
assert.equal(season.classify(rainfall(0),now),'dry');assert.equal(season.classify(rainfall(6),now),'wet');
const invalid=rainfall(1);invalid.daily.precipitation_sum[1]=null;assert.throws(()=>season.classify(invalid,now));
assert.throws(()=>season.classify(rainfall(0),new Date('2026-12-01T12:00:00Z')));
(async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(root,'index.html'),'utf8'),{url:'https://test.invalid/',runScripts:'outside-only'}),w=dom.window,d=w.document;
 const dialog=d.getElementById('dock-dialog');dialog.showModal=()=>dialog.open=true;dialog.close=()=>dialog.open=false;
 let altitude=-10,calls=0,interval;w.SeasonEstimate=season;w.previewHudLocation=()=>({latitude:-6.7,longitude:110.4});w.previewHudSunAltitude=()=>altitude;
 w.setInterval=cb=>{interval=cb};w.fetch=async()=>{calls++;return {ok:false}};
 w.eval(fs.readFileSync(path.join(root,'hud.js'),'utf8'));
 assert.equal(d.getElementById('hud-phase').alt,'Malam');assert.equal(d.getElementById('hud-battery').textContent,'--%');
 altitude=10;interval();assert.equal(d.getElementById('hud-phase').alt,'Siang');assert.equal(calls,1,'season requests are throttled');
 d.querySelector('[data-dock="bag"]').click();assert(dialog.open);assert.match(d.getElementById('dock-message').textContent,/belum dipulihkan/);
 d.getElementById('dock-close').click();assert(!dialog.open);
 for(const img of d.querySelectorAll('.character-hud img, .home-dock img'))assert(fs.existsSync(path.join(root,img.getAttribute('src'))));
 assert.equal(d.querySelectorAll('.character-hud-stats > div').length,4);
 await Promise.resolve();dom.window.close();console.log('HUD phase, clock, battery fallback, season validation, dock and assets: OK');
})().catch(error=>{console.error(error);process.exitCode=1});
