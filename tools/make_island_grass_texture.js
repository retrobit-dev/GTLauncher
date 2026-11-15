// Reproducible cozy grass-detail sprite sheet: four 320×160 transparent tile overlays.
// The 2× artwork adds drawn blades and flecks, not an enlargement of the earlier sheet.
const fs=require('fs'),path=require('path'),{createCanvas}=require('@napi-rs/canvas');
const W=160,H=80,S=2,canvas=createCanvas(W*S*4,H*S),ctx=canvas.getContext('2d');
ctx.scale(S,S);
function random(seed){let s=seed>>>0;return()=>{s^=s<<13;s^=s>>>17;s^=s<<5;return (s>>>0)/4294967296;}}
for(let frame=0;frame<4;frame++){
 const rand=random(21973+frame*7193),ox=frame*W;
 const inTile=(x,y)=>Math.abs((x-80)/80)+Math.abs((y-40)/40)<.68;
 // Subtle earth-toned speckles and pale highlights break up the flat grass surface.
 for(let i=0;i<140;i++){
  const x=Math.floor(26+rand()*108),y=Math.floor(14+rand()*52);if(!inTile(x,y))continue;
  ctx.fillStyle=i%5===0?'rgba(236,239,183,0.23)':'rgba(52,86,42,0.20)';
  ctx.fillRect(ox+x,y,(i%3===0?1.5:1),.5);
 }
 // Small varied three-blade tufts. Thicker roots and individually drawn tips stay
 // legible at 1× while the half-pixel steps supply detail at closer zooms.
 const tufts=[];
 for(let i=0;i<18;i++){
  let x,y,tries=0;do{x=34+Math.floor(rand()*92);y=17+Math.floor(rand()*46);tries++}
  while(tries<250 && (!inTile(x,y) || tufts.some(([px,py])=>((x-px)/7)**2+((y-py)/4)**2<1)));
  if(!inTile(x,y))continue;
  tufts.push([x,y]);
  const tall=3.5+Math.floor(rand()*3), sway=rand()<.5?-1:1;
  ctx.fillStyle='rgba(50,86,39,0.45)';
  ctx.fillRect(ox+x-2.5,y,5,.5);                         // soft root
  ctx.fillRect(ox+x-.5,y-tall,1,tall);                   // centre blade
  ctx.fillRect(ox+x-2,y-2.5,1,2.5);                      // left blade
  ctx.fillRect(ox+x+1,y-3,1,3);                          // right blade
  ctx.fillRect(ox+x+sway*.5,y-tall-.5,.5,1);            // tip bend
  ctx.fillStyle='rgba(221,236,164,0.44)';
  ctx.fillRect(ox+x+.5,y-tall+1,.5,Math.max(1,tall-2));
  ctx.fillRect(ox+x-2.5,y-3,.5,1);
  if(i%4===0){ctx.fillStyle='rgba(198,216,130,0.34)';ctx.fillRect(ox+x+2.5,y-1,.75,.75);}
 }
}
const buffer=canvas.toBuffer('image/png');
for(const name of ['preview/assets/island-grass-detail.png','app/src/main/res/drawable-nodpi/island_grass_detail.png']){
 fs.mkdirSync(path.dirname(name),{recursive:true});fs.writeFileSync(name,buffer);
}
console.log('Wrote shared grass detail sheet, 4 × 320×160:',buffer.length,'bytes');
