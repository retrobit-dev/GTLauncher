const assert=require('node:assert/strict'),{weights}=require('../preview/sky-blend');
assert.deepEqual(weights(-20),{night:1,twilight:0,day:0});assert.deepEqual(weights(20),{night:0,twilight:0,day:1});
for(let a=-22;a<15;a+=.1){const w=weights(a),next=weights(a+.001);assert(Math.abs(w.night+w.twilight+w.day-1)<1e-12);for(const key of ['night','twilight','day'])assert(Math.abs(w[key]-next[key])<.001);}
assert(weights(-10).twilight>0);assert(weights(0).twilight>0);console.log('Sky transition weights continuous and normalized: OK');
