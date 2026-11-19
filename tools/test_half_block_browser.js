const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({viewport:{width:1000,height:700},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const tiles=Array(400).fill(0),stacks=Array.from({length:400},()=>[]);stacks[210]=[1,1,1];tiles[210]=1;localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'height',worlds:[{id:'height',name:'Height',description:'',color:'#a8c795',gridSize:20,tiles,blockStacks:stacks,showCharacter:false,characterColumn:10,characterRow:10,characterScale:1,characterFacing:'S',house:null}]}));});await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});
 const report=await page.evaluate(()=>{const saved=localStorage.getItem('gtlauncher.worlds.v1'),w=worldManager.getActive(),p=sceneEditing.centre({column:10,row:10,blockHeight:1}),q=sceneEditing.centre({column:10,row:10,blockHeight:3}),out=[];
  const fill=CanvasRenderingContext2D.prototype.fill,stroke=CanvasRenderingContext2D.prototype.stroke;let fills=[],seams=[];
  CanvasRenderingContext2D.prototype.fill=function(...args){if(typeof this.fillStyle==='string')fills.push(this.fillStyle);return fill.apply(this,args)};
  CanvasRenderingContext2D.prototype.stroke=function(...args){if(Math.abs(this.lineWidth-.35)<.00001)seams.push(String(this.strokeStyle));return stroke.apply(this,args)};
  for(let material=1;material<=10;material++){
   let samples=[];for(const c of [10,11]){w.tiles.fill(0);for(let i=0;i<w.blockStacks.length;i++)w.blockStacks[i]=[];w.tiles[10*20+c]=material;w.blockStacks[10*20+c]=[material,material];fills=[];seams=[];sceneEditing.setGameplayDraft(null);samples.push({fills:[...new Set(fills)],seams:[...new Set(seams)]});}
   out.push({material,samples});
  }
  CanvasRenderingContext2D.prototype.fill=fill;CanvasRenderingContext2D.prototype.stroke=stroke;return{delta:p.y-q.y,saved,savedAfter:localStorage.getItem('gtlauncher.worlds.v1'),out};});
 assert.equal(report.delta,8,'two levels rise 8 units instead of 16');assert.equal(report.saved,report.savedAfter,'rendering does not rewrite old worlds');
 for(const {material,samples} of report.out){assert(samples.every(s=>s.seams.length>0),`missing side outlines for ${material}`);assert(samples[0].fills.some(x=>!samples[1].fills.includes(x)),`missing alternating color for ${material}`);}
 assert.deepEqual(errors,[]);console.log('Existing stacks: half height, side outlines, alternating colors and unchanged storage OK');await page.close();
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
