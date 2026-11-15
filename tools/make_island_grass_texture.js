// Reproducible high-resolution, transparent detail overlays for the island's grass tops.
// Output is a 4-frame 160x80 sprite sheet; both renderers consume the identical asset.
const fs=require('fs'),path=require('path'),{createCanvas}=require('@napi-rs/canvas');
const canvas=createCanvas(640,80),ctx=canvas.getContext('2d');
function random(seed){let s=seed>>>0;return()=>{s^=s<<13;s^=s>>>17;s^=s<<5;return (s>>>0)/4294967296;}}
for(let frame=0;frame<4;frame++){
 const rand=random(21973+frame*7193),ox=frame*160;
 const inTile=(x,y)=>Math.abs((x-80)/80)+Math.abs((y-40)/40)<.77;
 // Fine, low-contrast grass flecks, bounded well within the diamond to avoid seams.
 for(let i=0;i<100;i++){
  const x=Math.floor(24+rand()*112),y=Math.floor(11+rand()*58);if(!inTile(x,y))continue;
  ctx.fillStyle=i%4===0?'rgba(226,240,179,0.14)':'rgba(42,77,37,0.13)';
  ctx.fillRect(ox+x,y,1+(i%3===0?1:0),1);
 }
 // Soft little clusters: slender blades and subdued tips, with quiet empty space between.
 for(let i=0;i<12;i++){
  let x,y;do{x=30+Math.floor(rand()*100);y=14+Math.floor(rand()*52)}while(!inTile(x,y));
  ctx.fillStyle='rgba(42,78,39,0.28)';
  ctx.fillRect(ox+x,y-4,1,4);ctx.fillRect(ox+x-2,y-3,1,2);ctx.fillRect(ox+x+2,y-3,1,2);
  ctx.fillStyle='rgba(222,235,166,0.28)';ctx.fillRect(ox+x,y-5,1,1);
 }
}
const buffer=canvas.toBuffer('image/png');
for(const name of ['preview/assets/island-grass-detail.png','app/src/main/res/drawable-nodpi/island_grass_detail.png']){
 fs.mkdirSync(path.dirname(name),{recursive:true});fs.writeFileSync(name,buffer);
}
console.log('Wrote shared grass detail sheet, 4 × 160×80:',buffer.length,'bytes');
