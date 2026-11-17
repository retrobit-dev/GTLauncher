// Editor controls outside the tablet affect preview storage only, never Android or Base World.
(() => {
  'use strict';
  function columnLabel(column) { let n=column+1, label=''; while(n>0) { n--; label=String.fromCharCode(65+n%26)+label; n=Math.floor(n/26); } return label; }
  const stage=document.querySelector('.preview-stage'), canvas=document.getElementById('sky-canvas');
  const material=document.getElementById('terrain-material'), status=document.getElementById('terrain-status');
  const pan=document.getElementById('editor-pan'), scene=window.sceneEditing, worlds=window.worldManager;
  const blockPreviews={'1':document.getElementById('grass-block-preview'),'2':document.getElementById('dirt-block-preview')};
  function updateBlockPreview() { for(const [value,element] of Object.entries(blockPreviews)) if(element) element.hidden=material.value!==value; }
  material?.addEventListener('change',updateBlockPreview);
  updateBlockPreview();
  const buttons=Object.fromEntries(['draw','add','erase','pick','fill'].map(name=>[name,document.getElementById('terrain-'+name)]));
  if (!stage || !canvas || !material || !status || !pan || !scene?.tileAt || !worlds?.updateTiles || Object.values(buttons).some(x=>!x)) return;
  let tool=null, gesture=null, lastWorld=worlds.getActive().id;
  function select(next) {
    if(gesture && window.editorBlockHistory) window.editorBlockHistory.end();
    tool=next; gesture=null;
    for (const [name, button] of Object.entries(buttons)) button.setAttribute('aria-pressed', String(name===tool));
    canvas.classList.toggle('is-terrain-editing',Boolean(tool));
    const hint={draw:'Ketuk atau seret untuk mengganti block yang sudah ada.',add:'Ketuk atau seret untuk menambah block di atas block atau pada sel kosong.',erase:'Ketuk atau seret untuk menghapus block paling atas.',pick:'Ketuk block untuk mengambil jenisnya.',fill:'Ketuk untuk mengisi area block yang tersambung.'};
    status.textContent=tool ? hint[tool] : '';
  }
  for (const [name,button] of Object.entries(buttons)) button.addEventListener('click',()=>{
    if (pan.getAttribute('aria-pressed')==='true') pan.click();
    select(tool===name?null:name);
  });
  pan.addEventListener('click',()=>{if(pan.getAttribute('aria-pressed')==='true') select(null);});
  document.getElementById('mode-gameplay').addEventListener('click',()=>select(null));
  window.addEventListener('worldchange',()=>{const id=worlds.getActive().id;if(id!==lastWorld){lastWorld=id;select(null);}});
  function apply(event) {
    const world=worlds.getActive();
    if(world.id==='base'||![20,25,50,100].includes(world.gridSize)||!Array.isArray(world.tiles)) {
      status.textContent='Buat atau buka world 20×20, 25×25, 50×50 atau 100×100; Base dan world lama tidak diubah.';return;
    }
    const point=scene.tileAt(scene.toLocal(event.clientX,event.clientY));
    if(!point) {status.textContent='Di luar area world.';return;}
    const index=point.row*world.gridSize+point.column;
    if(gesture && gesture.visited.has(index)) return;
    if(gesture) gesture.visited.add(index);
    const previous=world.tiles[index], value=Number(material.value);
    if(tool==='pick') {
      if(previous===0){status.textContent='Tidak ada block untuk diambil.';return;}
      material.value=String(previous);updateBlockPreview();status.textContent='Jenis block diambil.';return;
    }
    if(tool==='draw' && previous===0) {status.textContent='Sel kosong. Gunakan Tambah block.';return;}
    const stacks=world.blockStacks ? world.blockStacks.map(stack=>stack.slice()) : world.tiles.map(tile=>tile?[tile]:[]);
    const stack=stacks[index];
    if(tool==='add') {
      if(stack.length>=32) {status.textContent='Tinggi maksimum 32 block.';return;}
      stack.push(value);
    } else if(tool==='erase') {
      if(!stack.length) {status.textContent='Tidak ada block untuk dihapus.';return;}
      stack.pop();
    } else if(tool==='draw') stack[stack.length-1]=value;
    else if(tool==='fill') {
      // Fill only connected top surfaces at the same height and material.
      const height=stack.length, target=previous, queue=[index],seen=new Set(queue);
      for(let i=0;i<queue.length;i++) {
        const n=queue[i], cell=stacks[n];
        if(height) cell[cell.length-1]=value; else cell.push(value);
        const x=n%world.gridSize,y=Math.floor(n/world.gridSize);
        for(const [dx,dy] of [[-1,0],[1,0],[0,-1],[0,1]]) {
          const nx=x+dx,ny=y+dy, j=ny*world.gridSize+nx;
          if(nx>=0 && nx<world.gridSize && ny>=0 && ny<world.gridSize && !seen.has(j) && stacks[j].length===height && world.tiles[j]===target) {seen.add(j);queue.push(j);}
        }
      }
    }
    const old=world.blockStacks || world.tiles.map(tile=>tile?[tile]:[]);
    if(stacks.every((cell,i)=>cell.length===old[i].length && cell.every((v,j)=>v===old[i][j]))) return;
    status.textContent=(window.editorBlockHistory ? window.editorBlockHistory.update(world.id,stacks) : worlds.updateBlockStacks(world.id,stacks))
      ? `${columnLabel(point.column)}${point.row+1}: ${tool==='erase' ? 'block dihapus' : tool==='add' || previous===0 ? 'block ditambahkan' : 'block diganti'}.`
      : 'Tidak disimpan: block digunakan karakter/rumah atau penyimpanan bermasalah.';
  }
  canvas.addEventListener('pointerdown',event=>{
    if(stage.dataset.mode!=='editor'||!tool||gesture||(event.pointerType==='mouse'&&event.button!==0))return;
    event.preventDefault();event.stopImmediatePropagation();
    if(window.editorBlockHistory && tool!=='pick' && !window.editorBlockHistory.begin()) { status.textContent='Tidak dapat mengubah block pada world ini.';return; }
    gesture={id:event.pointerId,visited:new Set()};
    try{canvas.setPointerCapture(event.pointerId);}catch(_){}
    apply(event);
  },true);
  window.addEventListener('pointermove',event=>{
    if(!gesture||gesture.id!==event.pointerId)return;
    event.preventDefault();event.stopPropagation();
    if(tool==='draw'||tool==='add'||tool==='erase')apply(event);
  },{capture:true,passive:false});
  function end(event){if(gesture&&gesture.id===event.pointerId){if(tool!=='pick')window.editorBlockHistory?.end();gesture=null;}}
  window.addEventListener('pointerup',end,true);window.addEventListener('pointercancel',end,true);
  canvas.addEventListener('lostpointercapture',end);
})();
