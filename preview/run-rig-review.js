(() => {
  'use strict';
  const canvas = document.getElementById('rig'), ctx = canvas.getContext('2d');
  const view = document.getElementById('view'), phase = document.getElementById('phase');
  const output = document.getElementById('frame'), play = document.getElementById('play'), showArm=document.getElementById('show-arm');
  const images = {front:new Image(),back:new Image()};
  images.front.src='./assets/main-character.png?v=hat-1';
  images.back.src='./assets/main-character-back.png?v=hat-1';
  // Experimental whole-arm art: source shoulder and elbow, not production pivots.
  const arms={front:{image:new Image(),shoulder:[390,370],elbow:[465,650]},back:{image:new Image(),shoulder:[460,365],elbow:[540,595]}};
  for(const [name,arm] of Object.entries(arms))arm.image.src=`./assets/character-redraw/run-rig/artwork-drafts/${name}-arm-transparent.png`;

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
    if(showArm.checked) {
      const arm=arms[view.value],a=data.views[view.value][index].near,b=data.views[view.value][next].near;
      const mix=name=>a[name].map((n,i)=>n+(b[name][i]-n)*blend);
      const shoulder=mix('shoulder'),elbow=mix('elbow');
      const sourceAngle=Math.atan2(arm.elbow[1]-arm.shoulder[1],arm.elbow[0]-arm.shoulder[0]);
      const targetAngle=Math.atan2(elbow[1]-shoulder[1],elbow[0]-shoulder[0]);
      const scale=29/Math.hypot(arm.elbow[0]-arm.shoulder[0],arm.elbow[1]-arm.shoulder[1]);
      if(arm.image.complete && arm.image.naturalWidth){ctx.save();ctx.translate(...shoulder);ctx.rotate(targetAngle-sourceAngle);ctx.scale(scale,scale);ctx.drawImage(arm.image,-arm.shoulder[0],-arm.shoulder[1]);ctx.restore();}
    }
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
  view.addEventListener('change',paint);showArm.addEventListener('change',paint);Object.values(images).forEach(image=>image.addEventListener('load',paint));Object.values(arms).forEach(arm=>arm.image.addEventListener('load',paint));
  fetch('./assets/character-redraw/run-rig/joints.json').then(response=>{if(!response.ok)throw new Error('Panduan sendi tidak dapat dimuat.');return response.json();}).then(result=>{data=result;paint();}).catch(()=>{const error=document.getElementById('error');error.textContent='Panduan sendi tidak dapat dimuat. Buka halaman melalui server preview.';error.hidden=false;});
  requestAnimationFrame(tick);
})();
