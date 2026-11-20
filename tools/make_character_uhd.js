// Increase the original standing sprite to 8x and add small, restrained hand-drawn
// highlights/seams. The original silhouette, palette family and character design remain.
const fs=require('fs'),path=require('path'),{createCanvas,loadImage}=require('@napi-rs/canvas');
const root=path.join(__dirname,'..');
const details={
 front:[
  // Hair glints, denim pocket/stitching and shoe highlights, at 4x-image coordinates.
  ['#b47b5e',.50,[[26,12,4,1],[31,10,3,1],[76,27,3,1],[80,31,2,1]]],
  ['#9bb1c4',.26,[[36,112,12,1],[36,114,1,5],[47,114,1,5],[38,120,8,1],[31,143,1,7],[57,143,1,7],[32,166,10,1],[56,166,10,1]]],
  ['#c8aa7f',.55,[[76,106,8,1],[77,123,7,1]]],
  ['#a36d4c',.55,[[23,181,8,1],[60,183,8,1]]]
 ],
 back:[
  ['#b87958',.50,[[26,11,5,1],[34,10,4,1],[76,28,4,1],[82,33,3,1]]],
  ['#e0a05c',.55,[[72,96,11,1],[84,99,6,1],[58,126,8,1],[81,127,8,1]]],
  ['#734b32',.58,[[72,111,9,1],[80,111,1,4],[59,135,7,1],[83,134,6,1]]],
  ['#93a9b9',.26,[[30,151,10,1],[54,152,10,1],[29,168,10,1],[56,168,10,1]]],
  ['#a36d4c',.55,[[20,184,8,1],[59,183,8,1]]]
 ]
};
(async()=>{for(const name of ['front','back']){
 const original=await loadImage(path.join(root,`app/src/main/res/drawable-nodpi/character_original_hd_${name}.png`));
 if(original.width!==108||original.height!==200)throw Error('Unexpected source sprite dimensions');
 const canvas=createCanvas(216,400),ctx=canvas.getContext('2d');ctx.imageSmoothingEnabled=false;ctx.drawImage(original,0,0,216,400);
 // Work in the 4x-image coordinate system at half-pixel precision; no shape changes.
 ctx.save();ctx.scale(2,2);
 for(const [color,alpha,marks] of details[name]) {ctx.fillStyle=color;ctx.globalAlpha=alpha;for(const [x,y,w,h] of marks)ctx.fillRect(x,y,w,h);}
 ctx.restore();
 // Clip every new detail to the original silhouette, preventing colored pixels outside it.
 const mask=createCanvas(216,400),m=mask.getContext('2d');m.imageSmoothingEnabled=false;m.drawImage(original,0,0,216,400);
 const maskPixels=m.getImageData(0,0,216,400);for(let i=3;i<maskPixels.data.length;i+=4)maskPixels.data[i]=maskPixels.data[i]?255:0;m.putImageData(maskPixels,0,0);
 ctx.globalCompositeOperation='destination-in';ctx.drawImage(mask,0,0);ctx.globalCompositeOperation='source-over';
 const bytes=canvas.toBuffer('image/png');
 for(const dest of [`preview/assets/character-original-uhd-${name}.png`,`app/src/main/res/drawable-nodpi/character_original_uhd_${name}.png`])fs.writeFileSync(path.join(root,dest),bytes);
 console.log(name,canvas.width+'x'+canvas.height,bytes.length+' bytes');
}})().catch(e=>{console.error(e);process.exitCode=1});
