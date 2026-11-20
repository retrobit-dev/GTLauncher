const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});try{
 const page=await browser.newPage({reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{const w={id:'check',name:'Check',description:'',color:'#a8c795',gridSize:20,tiles:Array(400).fill(1),showCharacter:false,characterColumn:1,characterRow:1,characterScale:1,characterFacing:'S',house:null,fences:[{column:10,row:10,edge:'E',material:'stone'},{column:10,row:10,edge:'N',material:'wood'}]};localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:w.id,worlds:[w]}));});
 await page.route('**/api.open-meteo.com/**',r=>r.abort());await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});
 const reports=await page.evaluate(()=>{const result=[];
  for(let q=0;q<4;q++){
   const world=worldManager.getActive(),fs=world.fences,size=world.gridSize;
   const projected=fs.map(f=>({...f,...WorldViewRotation.fence(f,size,q)})),joins=FenceJoins.plan(projected,()=>1);
   const meshes=fs.map((f,i)=>{const g=sceneEditing.fenceGeometry(f);return FenceMesh.faces(g.a,g.b,f.material,joins[i]);});
   const c=sceneEditing.centre(fs[0]);let overlap=null;
   for(let y=c.y-18;y<c.y+10 && !overlap;y+=.3)for(let x=c.x-16;x<c.x+16;x+=.3){if(meshes.every(faces=>faces.some(face=>FenceMesh.contains(face.points,x,y)))){overlap=sceneEditing.fenceAt({x,y});break;}}
   const depths=projected.map(f=>f.column+f.row+(['S','E'].includes(f.edge)?.5:-.5));
   const water={gridSize:size,tiles:Array(size*size).fill(0),blockStacks:Array.from({length:size*size},()=>[])};
   for(const [x,y,height] of [[10,10,3],[11,10,1],[10,11,2]]){const i=y*size+x;water.tiles[i]=3;water.blockStacks[i]=Array(height).fill(3);}
   const expected={};for(const [d,drop] of [['E',2],['S',1]])expected[{N:'backRight',E:'right',S:'left',W:'backLeft'}[WorldViewRotation.direction(d,q)]]=drop;
   result.push({q,overlap,depths,falls:sceneEditing.waterFalls(10,10,water),expected,top:sceneEditing.waterTopBorders(10,10,water),sides:sceneEditing.waterBorders(10,10,0,water)});
   sceneEditing.turnIsland(1);
  }return result;});
 for(const r of reports){assert.notEqual(r.overlap,null,`shared fence faces q${r.q}`);assert.equal(r.overlap,r.depths[0]===r.depths[1]?1:r.depths[0]>r.depths[1]?0:1,`front fence q${r.q}`);assert.deepEqual(r.falls,r.expected,`waterfall sides q${r.q}`);for(const [k,v] of Object.entries(r.top))assert.equal(v,!(k in r.expected),`water border ${k} q${r.q}`);assert.equal(r.sides.left,!('left' in r.expected));assert.equal(r.sides.right,!('right' in r.expected));}
 assert.deepEqual(errors,[]);console.log('Four-way overlapping fence selection and water-neighbour/waterfall orientation: OK');await page.close();
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1});
