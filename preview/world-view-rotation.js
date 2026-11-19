// Pure quarter-turn camera geometry. Never rotate or rewrite world records.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.WorldViewRotation=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const directions=['N','E','S','W'];
  const turns=n=>Number.isInteger(n)?((n%4)+4)%4:null;
  function point(column,row,size,quarterTurns) {
    const q=turns(quarterTurns);
    if(q===null || !Number.isInteger(size) || size<1 || !Number.isFinite(column) || !Number.isFinite(row))return null;
    let c=column,r=row;for(let i=0;i<q;i++){const next=size-1-r;r=c;c=next;}return {column:c,row:r};
  }
  function direction(facing,quarterTurns) {
    const q=turns(quarterTurns),i=directions.indexOf(facing);return q===null || i<0?null:directions[(i+q)%4];
  }
  function rectangle(column,row,width,height,size,quarterTurns) {
    const q=turns(quarterTurns);
    if(q===null || ![column,row,width,height,size].every(Number.isInteger) || size<1 || width<1 || height<1 || column<0 || row<0 || column+width>size || row+height>size)return null;
    let c=column,r=row,w=width,h=height;
    for(let i=0;i<q;i++){const nc=size-r-h,nr=c;[c,r,w,h]=[nc,nr,h,w];}
    return {column:c,row:r,width:w,height:h};
  }
  function fence(f,size,q) {
    if(!f || !directions.includes(f.edge) || !Number.isInteger(f.column) || !Number.isInteger(f.row) || f.column<0 || f.row<0 || f.column>=size || f.row>=size)return null;
    const p=point(f.column,f.row,size,q),edge=direction(f.edge,q);return p&&edge?{...p,edge}:null;
  }
  function bridge(b,size,q) {
    if(!b || !['E','S'].includes(b.axis) || ![3,4,5].includes(b.length??3) || !Number.isInteger(b.column) || !Number.isInteger(b.row))return null;
    const length=b.length??3,other={column:b.column+(b.axis==='E'?length-1:0),row:b.row+(b.axis==='S'?length-1:0)};
    if(b.column<0 || b.row<0 || other.column>=size || other.row>=size)return null;
    const a=point(b.column,b.row,size,q),z=point(other.column,other.row,size,q);if(!a||!z)return null;
    return {column:Math.min(a.column,z.column),row:Math.min(a.row,z.row),axis:a.row===z.row?'E':'S',length};
  }
  return Object.freeze({point,direction,rectangle,fence,bridge});
});
