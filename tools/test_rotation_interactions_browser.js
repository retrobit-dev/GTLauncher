const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:1200,height:900},reducedMotion:'reduce'}),errors=[];
  page.on('pageerror',e=>errors.push(e.stack||e.message));
  await page.addInitScript(()=>{
    if(localStorage.getItem('gtlauncher.worlds.v1'))return;
    const tiles=Array(2500).fill(1),stacks=tiles.map(t=>[t]);
    stacks[22*50+29]=[1,2,4];tiles[22*50+29]=4;
    stacks[29*50+28]=[3];tiles[29*50+28]=3;
    localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'rotation',worlds:[{id:'rotation',name:'Rotation',description:'',color:'#a8c795',gridSize:50,tiles,blockStacks:stacks,showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:{visible:true,column:30,row:25,scale:1,facing:'S'},orangeTrees:[{column:23,row:25},{column:21,row:25}],fences:[{column:27,row:24,edge:'S',material:'wood'}],bridges:[{column:27,row:29,axis:'E',material:'wood'}],ramps:[{column:26,row:26,direction:'E',material:2}]}]}));
  });
  await page.route('**/api.open-meteo.com/**',r=>r.abort());
  await page.goto('http://127.0.0.1:4173/',{waitUntil:'load'});
  const initial=await page.evaluate(()=>({world:JSON.stringify(worldManager.getActive()),storage:localStorage.getItem('gtlauncher.worlds.v1'),id:worldManager.getActive().id,objects:[worldManager.getActive().house,worldManager.getActive().orangeTrees,worldManager.getActive().fences,worldManager.getActive().bridges,worldManager.getActive().ramps]}));
  assert.equal(initial.id,'rotation');for(const value of initial.objects)assert(value,'fixture failed to load');
  await page.locator('#mode-map-editor').click();
  await page.evaluate(()=>sceneEditing.setCamera(2,0,40));
  for(let q=0;q<4;q++) {
    const report=await page.evaluate(()=>{
      const w=worldManager.getActive(), f=w.fences[0], g=sceneEditing.fenceGeometry(f), faces=FenceMesh.faces(g.a,g.b,f.material);
      let fenceHit=null;for(const face of faces){const p={x:face.points.reduce((a,v)=>a+v.x,0)/face.points.length,y:face.points.reduce((a,v)=>a+v.y,0)/face.points.length};if(sceneEditing.fenceAt(p)===0){fenceHit=p;break;}}
      const tree=w.orangeTrees[0], anchor=sceneEditing.orangeTreeAnchor(tree),footprint=[[0,0],[1,0],[0,1],[1,1]].map(([x,y])=>sceneEditing.centre({column:tree.column+x,row:tree.row+y}));
      const trees=w.orangeTrees,anchors=trees.map(t=>sceneEditing.orangeTreeAnchor(t));let overlap=null;
      const centreX=(anchors[0].x+anchors[1].x)/2,centreY=(anchors[0].y+anchors[1].y)/2;
      for(let y=centreY-65;y<centreY && !overlap;y+=1)for(let x=centreX-32;x<centreX+32;x+=1){const p={x,y};if(trees.every(t=>sceneEditing.treeContains(t,p))){overlap={selected:sceneEditing.orangeTreeAt(p),depth:trees.map(t=>{const v=WorldViewRotation.point(t.column+.5,t.row+.5,w.gridSize,sceneEditing.camera().quarterTurns);return v.column+v.row+1;})};break;}}
      const h=worldManager.getHouse(),hc=sceneEditing.centreHouse(h);let houseHit=false,houseMiss=false;
      for(let y=hc.y-100;y<hc.y+30 && !(houseHit&&houseMiss);y+=2)for(let x=hc.x-60;x<hc.x+60;x+=2){const hit=sceneEditing.hitHouse({x,y},h);houseHit ||=hit;houseMiss ||=!hit;}
      const bridge=w.bridges[0],bp=BridgeGeometry.cells(bridge).map(p=>sceneEditing.bridgeAt(sceneEditing.centre(p)));
      const raised=sceneEditing.centre({column:29,row:22,blockHeight:3});
      const ramp=sceneEditing.centre({column:26,row:26,blockHeight:1});
      const local=sceneEditing.centre({column:23,row:23}),canvas=document.getElementById('sky-canvas'),rect=canvas.getBoundingClientRect(),origin=sceneEditing.toLocal(rect.left,rect.top),z=sceneEditing.camera().renderZoom;
      return {q:sceneEditing.camera().quarterTurns,fenceHit,anchor,footprint,overlap,houseHit,houseMiss,bp,raised:sceneEditing.tileAt(raised),block:sceneEditing.blockAt(raised),ramp:sceneEditing.tileAt(ramp),client:{x:rect.left+(local.x-origin.x)*z*rect.width/320,y:rect.top+(local.y-origin.y)*z*rect.height/200},water:sceneEditing.waterTopBorders(28,29,w),world:JSON.stringify(w),storage:localStorage.getItem('gtlauncher.worlds.v1')};
    });
    assert.equal(report.q,q);assert(report.fenceHit,`fence picking q${q}`);assert(report.houseHit && report.houseMiss,`house alpha hit q${q}`);
    assert(report.overlap,`overlapping tree artwork q${q}`);
    const d=report.overlap.depth,front=d[0]===d[1]?1:d[0]>d[1]?0:1;assert.equal(report.overlap.selected,front,`tree painter order q${q}`);assert.deepEqual(report.bp,[0,0,0],`bridge picking q${q}`);
    assert(Math.abs(report.anchor.x-report.footprint.reduce((s,p)=>s+p.x,0)/4)<1e-8);assert(Math.abs(report.anchor.y-report.footprint.reduce((s,p)=>s+p.y,0)/4)<1e-8);
    assert.deepEqual(report.raised,{column:29,row:22},`raised top q${q}`);assert.deepEqual(report.block,{column:29,row:22,level:2},`selected level q${q}`);
    assert.deepEqual(report.ramp,{column:26,row:26},`ramp q${q}`);assert(Object.values(report.water).every(Boolean));assert.equal(report.world,initial.world,`rotation changed world q${q}`);assert.equal(report.storage,initial.storage,`rotation changed storage q${q}`);
    await page.evaluate(()=>sceneEditing.turnIsland(1));
  }
  async function tapTile(column,row){
    const point=await page.evaluate(({column,row})=>{const p=sceneEditing.centre({column,row}),r=document.querySelector('#sky-canvas').getBoundingClientRect(),o=sceneEditing.toLocal(r.left,r.top),z=sceneEditing.camera().renderZoom;return{x:r.left+(p.x-o.x)*z*r.width/320,y:r.top+(p.y-o.y)*z*r.height/200};},{column,row});
    const element=await page.evaluate(p=>document.elementFromPoint(p.x,p.y)?.id,point);
    assert.equal(element,'sky-canvas',`target ${column},${row} obscured by ${element}`);await page.mouse.click(point.x,point.y);
  }
  console.log('geometry verified');
  await page.locator('#terrain-draw').click({timeout:10000});
  await page.evaluate(()=>{const select=document.getElementById('terrain-material');select.value='5';select.dispatchEvent(new Event('change',{bubbles:true}));});
  for(let q=0;q<4;q++){
    const c=22+q,r=21;const before=await page.evaluate(()=>worldManager.getActive().tiles.slice());
    console.log(`editor turn ${q}`);await tapTile(c,r);
    const after=await page.evaluate(()=>worldManager.getActive().tiles.slice());
    assert.equal(after[r*50+c],5,`editor tap q${q}`);
    assert.deepEqual(after.map((v,i)=>v!==before[i]?i:-1).filter(i=>i>=0),[r*50+c],`editor modified wrong tile q${q}`);
    await page.evaluate(()=>sceneEditing.turnIsland(1));
  }
  console.log('editor verified');await page.evaluate(()=>document.getElementById('mode-gameplay').click());
  for(let q=0;q<4;q++){
    const dest={column:25+(q%2),row:22+(q>=2?1:0)};
    const start={column:dest.column===25?26:25,row:dest.row};
    await page.evaluate(start=>{const w=worldManager.getActive();if(!worldManager.updateCharacter(w.id,{...worldManager.getCharacter(),...start}))throw Error('failed to set start');},start);
    console.log(`gameplay turn ${q}`);await tapTile(dest.column,dest.row);
    const character=await page.evaluate(()=>worldManager.getCharacter());
    assert.equal(character.column,dest.column,`gameplay column q${q}`);assert.equal(character.row,dest.row,`gameplay row q${q}`);
    await page.evaluate(()=>sceneEditing.turnIsland(1));
  }
  const savedCharacter=await page.evaluate(()=>worldManager.getCharacter());
  await page.reload({waitUntil:'domcontentloaded',timeout:90000});
  await page.waitForFunction(()=>window.worldManager && window.sceneEditing,null,{timeout:90000});
  const reloaded=await page.evaluate(()=>({w:worldManager.getActive(),character:worldManager.getCharacter(),turns:sceneEditing.camera().quarterTurns}));
  for(let c=22;c<26;c++)assert.equal(reloaded.w.tiles[21*50+c],5,`edited tile ${c} persisted`);
  assert.deepEqual(reloaded.character,savedCharacter);assert.equal(reloaded.turns,0,'camera rotation is transient');
  assert.deepEqual(errors,[]);
  console.log('Four camera orientations: raised block, ramp, fence/bridge picking, tree footprint, water borders, saved world invariants, editor taps and gameplay movement: OK');
  await page.close();
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
