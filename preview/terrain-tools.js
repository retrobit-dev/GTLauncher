// Editor controls outside the tablet affect preview storage only, never Android or Base World.
(() => {
  'use strict';
  function columnLabel(column) { let n=column+1, label=''; while(n>0) { n--; label=String.fromCharCode(65+n%26)+label; n=Math.floor(n/26); } return label; }
  const stage=document.querySelector('.preview-stage'), canvas=document.getElementById('sky-canvas');
  const material=document.getElementById('terrain-material'), status=document.getElementById('terrain-status');
  const pan=document.getElementById('editor-pan'), scene=window.sceneEditing, worlds=window.worldManager;
  const buttons=Object.fromEntries(['draw','erase','pick','fill'].map(name=>[name,document.getElementById('terrain-'+name)]));
  if (!stage || !canvas || !material || !status || !pan || !scene?.tileAt || !worlds?.updateTiles || Object.values(buttons).some(x=>!x)) return;
  let tool=null, gesture=null, lastWorld=worlds.getActive().id;
  function select(next) {
    tool=next; gesture=null;
    for (const [name, button] of Object.entries(buttons)) button.setAttribute('aria-pressed', String(name===tool));
    canvas.classList.toggle('is-terrain-editing',Boolean(tool));
    const hint={draw:'Ketuk atau seret untuk menambah atau mengganti block di dalam grid.',erase:'Ketuk atau seret untuk menghapus block.',pick:'Ketuk block untuk mengambil jenisnya.',fill:'Ketuk untuk mengisi area block yang tersambung.'};
    status.textContent=tool ? hint[tool] : '';
  }
  for (const [name,button] of Object.entries(buttons)) button.addEventListener('click',()=>{
    if (pan.getAttribute('aria-pressed')==='true') pan.click();
    select(tool===name?null:name);
  });
  pan.addEventListener('click',()=>{if(pan.getAttribute('aria-pressed')==='true') select(null);});
  document.getElementById('mode-gameplay').addEventListener('click',()=>select(null));
  window.addEventListener('worldchange',()=>{const id=worlds.getActive().id;if(id!==lastWorld){lastWorld=id;select(null);}});
  function fill(tiles,start,value,size) {
    const target=tiles[start];if(target===value)return;
    const queue=[start];tiles[start]=value;
    for(let i=0;i<queue.length;i++) {
      const at=queue[i],x=at%size,y=Math.floor(at/size);
      for(const [dx,dy] of [[-1,0],[1,0],[0,-1],[0,1]]) {
        const nx=x+dx,ny=y+dy;if(nx<0||nx>=size||ny<0||ny>=size)continue;
        const n=ny*size+nx;if(tiles[n]===target){tiles[n]=value;queue.push(n);}
      }
    }
  }
  function apply(event) {
    const world=worlds.getActive();
    if(world.id==='base'||![20,25,50,100].includes(world.gridSize)||!Array.isArray(world.tiles)) {
      status.textContent='Buat atau buka world 20×20, 25×25, 50×50 atau 100×100; Base dan world lama tidak diubah.';return;
    }
    const point=scene.tileAt(scene.toLocal(event.clientX,event.clientY));
    if(!point) {status.textContent='Di luar area world.';return;}
    const index=point.row*world.gridSize+point.column;
    if(gesture && gesture.last===index) return;
    if(gesture) gesture.last=index;
    const previous=world.tiles[index], value=Number(material.value);
    if(tool==='pick') {
      if(previous===0){status.textContent='Tidak ada block untuk diambil.';return;}
      material.value=String(previous);status.textContent='Jenis block diambil.';return;
    }
    const next=world.tiles.slice();
    if(tool==='fill') fill(next,index,value,world.gridSize);
    else next[index]=tool==='erase'?0:value;
    if(next.every((v,i)=>v===world.tiles[i])) return;
    status.textContent=worlds.updateTiles(world.id,next)
      ? `${columnLabel(point.column)}${point.row+1}: ${tool==='erase' ? 'block dihapus' : previous===0 ? 'block ditambahkan' : 'block diganti'}.`
      : 'Tidak disimpan: block digunakan karakter/rumah atau penyimpanan bermasalah.';
  }
  canvas.addEventListener('pointerdown',event=>{
    if(stage.dataset.mode!=='editor'||!tool||gesture||(event.pointerType==='mouse'&&event.button!==0))return;
    event.preventDefault();event.stopImmediatePropagation();
    gesture={id:event.pointerId,last:-1};
    try{canvas.setPointerCapture(event.pointerId);}catch(_){}
    apply(event);
  },true);
  window.addEventListener('pointermove',event=>{
    if(!gesture||gesture.id!==event.pointerId)return;
    event.preventDefault();event.stopPropagation();
    if(tool==='draw'||tool==='erase')apply(event);
  },{capture:true,passive:false});
  function end(event){if(gesture&&gesture.id===event.pointerId)gesture=null;}
  window.addEventListener('pointerup',end,true);window.addEventListener('pointercancel',end,true);
  canvas.addEventListener('lostpointercapture',end);
})();
