// Cozy meadow grass: four transparent 320×160 tile overlays, including one empty variant.
// Each motif is hand-placed pixel art with leaves and a stem; there are no flecks or flowers.
const fs=require('fs'),path=require('path'),{createCanvas}=require('@napi-rs/canvas');
const W=160,H=80,S=2,canvas=createCanvas(W*S*4,H*S),ctx=canvas.getContext('2d');
ctx.scale(S,S);
// Positions stay well inside each diamond so adjacent tile edges remain clean.
const patterns=[[],[[55,36],[101,43]],[[76,28],[100,44],[55,46]],[[63,31],[88,47]]];
function tuft(x,y,variation){
 // A tiny V-shaped plant: paired leaves in soft spring greens, with a short darker stem.
 const flip=variation%2===0?1:-1;
 ctx.save();ctx.translate(x,y);ctx.scale(flip*4,4);
 ctx.fillStyle='rgba(91,124,96,0.62)';ctx.fillRect(-.5,0,1,3);ctx.fillRect(-1,2,2,1);
 ctx.fillStyle='rgba(111,146,120,0.78)';ctx.fillRect(-3,-1,2,2);ctx.fillRect(1,-2,2,2);ctx.fillRect(-2,1,2,1);ctx.fillRect(0,0,2,1);
 ctx.fillStyle='rgba(196,216,183,0.8)';ctx.fillRect(-3,-2,1,1);ctx.fillRect(2,-3,1,1);ctx.fillRect(-2,-1,1,1);ctx.fillRect(1,-2,1,1);
 ctx.restore();
}
for(let frame=0;frame<4;frame++){
 const x=frame*W;for(let i=0;i<patterns[frame].length;i++)tuft(x+patterns[frame][i][0],patterns[frame][i][1],frame+i);
}
const buffer=canvas.toBuffer('image/png');
for(const name of ['preview/assets/island-grass-detail-large.png','app/src/main/res/drawable-nodpi/island_grass_detail.png']){
 fs.mkdirSync(path.dirname(name),{recursive:true});fs.writeFileSync(name,buffer);
}
console.log('Wrote shared meadow grass sheet: empty + 3 sparse 320×160 variants:',buffer.length,'bytes');
