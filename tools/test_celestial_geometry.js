const assert=require('node:assert/strict'),g=require('../preview/celestial-geometry');
const close=(a,b)=>assert.ok(Math.abs(a-b)<1e-8,`${a} != ${b}`);
let rise=g.offset(-90,0,0),peak=g.offset(0,0,0),set=g.offset(90,0,0);
close(rise.x,300);close(rise.y,0);close(peak.x,0);close(peak.y,-175);close(set.x,-300);
close(g.shadowScale(0,0,0,24),175/151);close(g.shadowScale(90,0,0,24),300/276);
for(const h of [10,100,600])close(g.shadow(250,14,h)+h,250-7+50);
const polar=g.offset(0,23,89);assert.ok(Number.isFinite(polar.x)&&Number.isFinite(polar.y));
console.log('Celestial orbit, 175 height, shadow scale and 50-unit lower edge: OK');
