// Editor controls outside the tablet affect preview storage only, never Android or Base World.
(() => {
  'use strict';
  function columnLabel(column) { let n=column+1, label=''; while(n>0) { n--; label=String.fromCharCode(65+n%26)+label; n=Math.floor(n/26); } return label; }
  const stage=document.querySelector('.preview-stage'), canvas=document.getElementById('sky-canvas');
  const material=document.getElementById('terrain-material'), status=document.getElementById('terrain-status');
  const pan=document.getElementById('editor-pan'), scene=window.sceneEditing, worlds=window.worldManager;
  const blockPreviews={'1':document.getElementById('grass-block-preview'),'2':document.getElementById('dirt-block-preview'),'3':document.getElementById('water-block-preview'),'4':document.getElementById('stone-block-preview'),'5':document.getElementById('sand-block-preview'),...Object.fromEntries([6,7,8,9,10].map(n=>[String(n),document.getElementById('block-preview-'+n)]))};
  const picker=document.getElementById('block-picker'), trigger=document.getElementById('block-picker-trigger');
  const pickerList=document.getElementById('block-picker-list'), pickerValue=document.getElementById('block-picker-value');
  const options=pickerList ? [...pickerList.querySelectorAll('[role="option"]')] : [];
  function updateBlockPreview() {
    for(const [value,element] of Object.entries(blockPreviews)) if(element) element.hidden=material.value!==value;
    if(pickerValue) pickerValue.textContent=material.selectedOptions[0]?.textContent || '';
    for(const option of options) option.setAttribute('aria-selected',String(option.dataset.blockValue===material.value));
  }
  function closePicker(focus=false) {if(!pickerList || !trigger)return;pickerList.hidden=true;trigger.setAttribute('aria-expanded','false');if(focus)trigger.focus();}
  function openPicker() {
    if(!pickerList || !trigger)return;pickerList.hidden=false;trigger.setAttribute('aria-expanded','true');
    (options.find(option=>option.dataset.blockValue===material.value)||options[0])?.focus({preventScroll:true});
    // Keep the list reachable in the toolbar's scroll area on short screens.
    const panel=picker.closest('.editor-right-controls');if(panel) panel.scrollTop=Math.max(0,pickerList.offsetTop+picker.offsetTop+pickerList.offsetHeight-panel.clientHeight);
  }
  material?.addEventListener('change',updateBlockPreview);
  trigger?.addEventListener('click',()=>pickerList.hidden?openPicker():closePicker());
  trigger?.addEventListener('keydown',event=>{if((event.key==='ArrowDown'||event.key==='ArrowUp') && pickerList.hidden){event.preventDefault();openPicker();}});
  for(const option of options) option.addEventListener('click',()=>{material.value=option.dataset.blockValue;material.dispatchEvent(new Event('change',{bubbles:true}));closePicker(true);});
  pickerList?.addEventListener('keydown',event=>{
    if(event.key==='Escape'){event.preventDefault();closePicker(true);return;}
    if(!['ArrowDown','ArrowUp','Home','End'].includes(event.key))return;
    event.preventDefault();const current=options.indexOf(document.activeElement);
    const index=event.key==='Home'?0:event.key==='End'?options.length-1:(current+(event.key==='ArrowDown'?1:-1)+options.length)%options.length;
    options[index]?.focus();
  });
  document.addEventListener('pointerdown',event=>{if(picker && !picker.contains(event.target))closePicker();},true);
  document.getElementById('mode-gameplay')?.addEventListener('click',()=>closePicker());
  updateBlockPreview();
  const buttons=Object.fromEntries(['draw','add','erase','pick','fill','ramp'].map(name=>[name,document.getElementById('terrain-'+name)]));
  if (!stage || !canvas || !material || !status || !pan || !scene?.tileAt || !worlds?.updateTiles || Object.values(buttons).some(x=>!x)) return;
  let tool=null, gesture=null, selectedRamp=null, lastWorld=worlds.getActive().id;
  function select(next) {
    if(gesture && window.editorBlockHistory) window.editorBlockHistory.end();
    tool=next; gesture=null;selectedRamp=null;
    for (const [name, button] of Object.entries(buttons)) button.setAttribute('aria-pressed', String(name===tool));
    canvas.classList.toggle('is-terrain-editing',Boolean(tool));
    document.getElementById('terrain-ramp-rotate').hidden=tool!=='ramp';
    const hint={draw:'Ketuk atau seret untuk mengganti block yang sudah ada.',add:'Ketuk atau seret untuk menambah block di atas block atau pada sel kosong.',erase:'Ketuk atau seret untuk menghapus block paling atas.',pick:'Ketuk block untuk mengambil jenisnya.',fill:'Ketuk untuk mengisi area block yang tersambung.',ramp:'Pilih material, letakkan atau pilih ramp, lalu gunakan tombol putar atau hapus.'};
    status.textContent=tool ? hint[tool] : '';
  }
  for (const [name,button] of Object.entries(buttons)) button.addEventListener('click',()=>{
    if (pan.getAttribute('aria-pressed')==='true') pan.click();
    select(tool===name?null:name);
  });
  for(const [id,step] of [['ramp-rotate-left',-1],['ramp-rotate-right',1]])document.getElementById(id).addEventListener('click',()=>{
    const direction=document.getElementById('terrain-ramp-direction'),dirs=['N','E','S','W'];
    const world=worlds.getActive(),before=world.ramps||[],current=selectedRamp && before.find(r=>r.column===selectedRamp.column && r.row===selectedRamp.row);
    const nextDirection=dirs[(dirs.indexOf(current?.direction ?? direction.value)+step+4)%4];
    if(current){const next=before.map(r=>r===current?{...r,direction:nextDirection}:{...r});if(!window.editorRampHistory?.update(world.id,next)){status.textContent='Ramp tidak dapat diputar.';return;}}
    direction.value=nextDirection;status.textContent=current?'Ramp diputar.':'Arah ramp diputar; ketuk block penyangga.';
  });
  document.getElementById('ramp-remove').addEventListener('click',()=>{
    const world=worlds.getActive(),before=world.ramps||[],current=selectedRamp && before.find(r=>r.column===selectedRamp.column && r.row===selectedRamp.row);
    if(!current){status.textContent='Pilih ramp yang akan dihapus.';return;}
    if(window.editorRampHistory?.update(world.id,before.filter(r=>r!==current).map(r=>({...r})))){selectedRamp=null;status.textContent='Ramp dihapus.';}else status.textContent='Ramp tidak dapat dihapus.';
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
    if(tool==='ramp') {
      if(!previous){status.textContent='Ramp membutuhkan block penyangga.';return;}
      const direction=document.getElementById('terrain-ramp-direction').value,before=world.ramps||[],existing=before.find(r=>r.column===point.column && r.row===point.row);
      if(existing){selectedRamp={...point};document.getElementById('terrain-ramp-direction').value=existing.direction;status.textContent='Ramp dipilih; putar atau hapus dengan tombol.';return;}
      const next=before.map(r=>({...r}));next.push({...point,direction,material:value});
      const saved=window.editorRampHistory?.update(world.id,next);
      if(saved)selectedRamp={...point};
      status.textContent=saved ? 'Ramp disimpan; gunakan tombol putar untuk mengubah arahnya.' : 'Ramp tidak disimpan; periksa block, player, dan objek pada tapak.';
      return;
    }
    if(tool==='pick') {
      if(previous===0){status.textContent='Tidak ada block untuk diambil.';return;}
      material.value=String(previous);updateBlockPreview();status.textContent='Jenis block diambil.';return;
    }
    if(tool==='draw' && previous===0) {status.textContent='Sel kosong. Gunakan Tambah block.';return;}
    const stacks=world.blockStacks ? world.blockStacks.map(stack=>stack.slice()) : world.tiles.map(tile=>tile?[tile]:[]);
    const stack=stacks[index];
    if(tool==='add') {
      if(stack.length>=5) {status.textContent='Tinggi maksimum 5 block.';return;}
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
    if(window.editorBlockHistory && tool!=='pick' && tool!=='ramp' && !window.editorBlockHistory.begin()) { status.textContent='Tidak dapat mengubah block pada world ini.';return; }
    gesture={id:event.pointerId,visited:new Set()};
    try{canvas.setPointerCapture(event.pointerId);}catch(_){}
    apply(event);
  },true);
  window.addEventListener('pointermove',event=>{
    if(!gesture||gesture.id!==event.pointerId)return;
    event.preventDefault();event.stopPropagation();
    if(tool==='draw'||tool==='add'||tool==='erase')apply(event);
  },{capture:true,passive:false});
  function end(event){if(gesture&&gesture.id===event.pointerId){if(tool!=='pick' && tool!=='ramp')window.editorBlockHistory?.end();gesture=null;}}
  window.addEventListener('pointerup',end,true);window.addEventListener('pointercancel',end,true);
  canvas.addEventListener('lostpointercapture',end);
})();
