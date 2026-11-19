const assert=require('node:assert/strict'),{chromium}=require('playwright-core');
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--disable-dev-shm-usage','--use-gl=angle','--use-angle=swiftshader']});
  try {
    for(const width of [1000,360]) {
      const page=await browser.newPage({viewport:{width,height:700},reducedMotion:'reduce'}),errors=[];
      page.on('pageerror',e=>errors.push(e.message));page.setDefaultTimeout(60000);
      await page.addInitScript(()=>{
        const tiles=Array(2500).fill(1);
        localStorage.setItem('gtlauncher.worlds.v1',JSON.stringify({activeId:'catalog',worlds:[{id:'catalog',name:'Kebun',description:'',color:'#a8c795',gridSize:50,tiles,showCharacter:true,characterColumn:24,characterRow:24,characterScale:1,characterFacing:'S',house:null}]}));
      });
      await page.route('**/api.open-meteo.com/**',r=>r.abort());
      await page.goto(process.env.PREVIEW_URL||'http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
      await page.locator('#mode-map-editor').click();await page.locator('#object-library-button').click();
      assert.equal(await page.locator('#catalog-buildings-title').innerText(),'Bangunan');
      assert.equal(await page.locator('#catalog-trees-title').innerText(),'Tanaman dan pohon');
      assert.equal(await page.locator('#catalog-trees-title + .catalog-grid .catalog-tile').count(),5);
      const geom=await page.evaluate(()=>{
        const dialog=document.querySelector('#object-library-dialog'),a=document.querySelector('#catalog-place-orange').getBoundingClientRect(),b=document.querySelector('#catalog-place-conifer').getBoundingClientRect();
        return {scroll:dialog.scrollWidth,client:dialog.clientWidth,a:{left:a.left,top:a.top,right:a.right,width:a.width},b:{left:b.left,top:b.top,right:b.right,width:b.width}};
      });
      assert(geom.scroll<=geom.client,'dialog has no horizontal overflow');
      assert(Math.abs(geom.a.top-geom.b.top)<1 && geom.a.right<=geom.b.left,'trees are in the same grid row');
      assert(geom.a.width>=44 && geom.b.width>=44,'cards have touch-friendly target sizes');
      const house=page.getByRole('button',{name:'Tempatkan rumah player, tapak 3 kali 3'});
      assert(await house.isEnabled());
      const tree=page.getByRole('button',{name:'Tempatkan pohon cemara, tapak 2 kali 2'});
      await tree.focus();await page.keyboard.press('Enter');
      assert.equal(await page.locator('#object-library-dialog').evaluate(e=>e.open),false,'keyboard selection begins placement');
      assert.match(await page.locator('#edit-status').innerText(),/pohon cemara/);
      assert.deepEqual(errors,[]);await page.close();
    }
    console.log('Grouped catalog grid, responsive layout and keyboard placement: OK');
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
