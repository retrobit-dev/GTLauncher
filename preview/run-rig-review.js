(() => {
  'use strict';
  const canvas = document.getElementById('rig'), ctx = canvas.getContext('2d');
  const view = document.getElementById('view'), phase = document.getElementById('phase');
  const output = document.getElementById('frame'), play = document.getElementById('play');
  const images = {front:new Image(),back:new Image()};
  images.front.src='./assets/main-character.png?v=hat-1';
  images.back.src='./assets/main-character-back.png?v=hat-1';
  const reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
  let data, running=!reduced, time=null, position=0;
  play.textContent=running?'Jeda':'Putar';
  function paint() {
    if(!data)return;
    const index=Math.floor(position)%12, next=(index+1)%12, blend=position-index;
    ctx.clearRect(0,0,432,800);
    const image=images[view.value];
    if(image.complete && image.naturalWidth){ctx.save();ctx.globalAlpha=.30;ctx.drawImage(image,0,0,432,800);ctx.restore();}
    ctx.save();ctx.scale(2,2);ctx.lineCap='round';ctx.lineJoin='round';
    for(const side of ['far','near']) {
      const a=data.views[view.value][index][side],b=data.views[view.value][next][side];
      const point=name=>a[name].map((n,i)=>n+(b[name][i]-n)*blend);
      ctx.strokeStyle=side==='near'?'#ab422a':'#16669c';ctx.fillStyle=ctx.strokeStyle;ctx.lineWidth=3;
      for(const chain of [['shoulder','elbow','wrist'],['hip','knee','ankle']]) {
        ctx.beginPath();chain.forEach((name,i)=>{const [x,y]=point(name);if(i)ctx.lineTo(x,y);else ctx.moveTo(x,y);});ctx.stroke();
        for(const name of chain){const [x,y]=point(name);ctx.beginPath();ctx.arc(x,y,3,0,Math.PI*2);ctx.fill();}
      }
    }
    ctx.restore();phase.value=String(index);output.textContent=`${index+1} / 12`;
  }
  function tick(now){if(running && data){if(time!==null)position=(position+(now-time)*12/800)%12;time=now;paint();}else time=null;requestAnimationFrame(tick);}
  play.addEventListener('click',()=>{running=!running;time=null;play.textContent=running?'Jeda':'Putar';});
  phase.addEventListener('input',()=>{running=false;play.textContent='Putar';position=Number(phase.value);paint();});
  view.addEventListener('change',paint);Object.values(images).forEach(image=>image.addEventListener('load',paint));
  fetch('./assets/character-redraw/run-rig/joints.json').then(response=>{if(!response.ok)throw new Error('Panduan sendi tidak dapat dimuat.');return response.json();}).then(result=>{data=result;paint();}).catch(()=>{const error=document.getElementById('error');error.textContent='Panduan sendi tidak dapat dimuat. Buka halaman melalui server preview.';error.hidden=false;});
  requestAnimationFrame(tick);
})();
