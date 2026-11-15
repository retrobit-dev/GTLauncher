const assert=require('assert'),hair=require('../preview/character-hair-motion');
assert.equal(hair.offset(.16,1,1,1),0,'face and below do not move');
assert.equal(hair.offset(0,1,1,1,false),0,'reduced motion');
assert.equal(hair.offset(0,0,1,1),0,'stationary phase');
assert.equal(hair.offset(0,1,0,1),.2);
assert(Math.abs(hair.offset(0,1,1,1)-.32)<1e-10,'run is stronger');
assert.equal(hair.offset(.04,1,0,1),-hair.offset(.04,-1,0,1),'opposite phases reverse');
assert(hair.offset(.08,1,1,1)<hair.offset(.02,1,1,1),'motion fades towards hairline');
assert.equal(hair.offset(.5,1,1,1),0);assert.equal(hair.offset(NaN,1,1,1),0);
const fs=require('fs'),vm=require('vm'),path=require('path'),{createCanvas,Image}=require('@napi-rs/canvas');
const root=path.join(__dirname,'../preview');
let src=fs.readFileSync(path.join(root,'app.js'),'utf8').replace('  drawFrame();\n  initializeWeather();','  window.drawCharacterForHairTest=drawMainCharacter;');
const canvas=createCanvas(640,400);canvas.clientWidth=640;canvas.addEventListener=()=>{};let time=0,character={visible:true,column:4,row:4,scale:1,facing:'S'};
const noop=()=>{},w={matchMedia:()=>({matches:false}),requestAnimationFrame:noop,setInterval:noop,addEventListener:noop,CharacterHairMotion:hair,worldManager:{getActive:()=>({id:'world'}),getCharacter:()=>({...character}),getHouse:()=>null,getFences:()=>[],getRoads:()=>[],getBushes:()=>[],getTrees:()=>[],updateCharacter:(id,c)=>{character={...c};return true}}};
class Sprite extends Image{set src(v){super.src=fs.readFileSync(path.join(root,'assets',path.basename(v)))}}
const sb={console,document:{getElementById:id=>id==='sky-canvas'?canvas:null,createElement:()=>createCanvas(600,400)},window:w,navigator:{},performance:{now:()=>time},fetch:async()=>({ok:false}),Map,Date,Image:Sprite};vm.createContext(sb);vm.runInContext(src,sb);w.sceneEditing.setEditorZoom(.2);
(async()=>{await new Promise(r=>setTimeout(r,150));assert(w.sceneEditing.walkTo({column:5,row:4}));time=75;
 const ctx=canvas.getContext('2d'),draw=ctx.drawImage;
 function headBands(enabled){w.CharacterHairMotion=enabled?hair:null;const calls=[];ctx.drawImage=function(...args){if(args.length===9&&args[1]===0&&args[2]<64&&args[4]>0)calls.push(args);return draw.apply(this,args)};w.drawCharacterForHairTest(0);ctx.drawImage=draw;return calls;}
 const moving=headBands(true),still=headBands(false);assert.equal(moving.length,8);assert.equal(still.length,8);
 assert(moving.some((c,i)=>Math.abs(c[5]-still[i][5])>.01),'hair crown shifts while moving');
 assert(moving.every(c=>c[2]+c[4]<=64),'only the top 16 percent of sprite is deformed');
 console.log('Hair: subtle gait-linked crown motion, reduced-motion guard, face and backpack unchanged: OK');
})().catch(e=>{console.error(e);process.exitCode=1});
