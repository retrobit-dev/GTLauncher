// Rainfall-based non-official season estimate, matching Android's three complete dasarian rule.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.SeasonEstimate=factory();})(typeof globalThis!=='undefined'?globalThis:this,function(){
  'use strict';
  const DAY=86400000;
  function classify(data,now=new Date()) {
    const dates=data?.daily?.time, rain=data?.daily?.precipitation_sum;
    if(!Array.isArray(dates)||!Array.isArray(rain)||dates.length<60||dates.length!==rain.length) throw Error('Incomplete rainfall');
    const periods=[];let previous=null;
    for(let i=0;i<dates.length;i++) {
      if(typeof dates[i]!=='string'||!/^\d{4}-\d\d-\d\d$/.test(dates[i])) throw Error('Invalid date');
      const t=Date.parse(dates[i]+'T00:00:00Z'),d=new Date(t),n=rain[i];
      if(!Number.isFinite(t)||d.toISOString().slice(0,10)!==dates[i]||!Number.isFinite(n)||n<0||(previous!==null&&t!==previous+DAY)) throw Error('Invalid rainfall');
      previous=t;const day=d.getUTCDate(),part=day<=10?1:day<=20?2:3,month=dates[i].slice(0,7);
      let p=periods[periods.length-1];if(!p||p.month!==month||p.part!==part) {p={month,part,first:day,last:day,sum:0,year:d.getUTCFullYear(),monthIndex:d.getUTCMonth()};periods.push(p);}
      p.last=day;p.sum+=n;
    }
    const today=Date.UTC(now.getUTCFullYear(),now.getUTCMonth(),now.getUTCDate());
    if(previous>today||previous<today-9*DAY) throw Error('Stale rainfall');
    const complete=periods.filter(p=>p.first===(p.part===1?1:p.part===2?11:21)&&p.last===(p.part===1?10:p.part===2?20:new Date(Date.UTC(p.year,p.monthIndex+1,0)).getUTCDate()));
    let season='unknown';for(let i=2;i<complete.length;i++){const last=complete.slice(i-2,i+1);if(last.every(p=>p.sum>=50))season='wet';else if(last.every(p=>p.sum<50))season='dry';}
    return season;
  }
  return Object.freeze({classify});
});
