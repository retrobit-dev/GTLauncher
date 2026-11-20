// Map-editor region numbering; coordinates are always world-space, not camera-space.
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.EditorGridRegions=factory();})(typeof window!=='undefined'?window:globalThis,()=>{
  'use strict';
  const REGION_SIZE=10;
  function regions(size){
    if(!Number.isInteger(size)||size<1)return [];
    const count=Math.ceil(size/REGION_SIZE),out=[];
    for(let row=0;row<count;row++)for(let column=0;column<count;column++){
      const c=column*REGION_SIZE,r=row*REGION_SIZE;
      out.push({number:row*count+column+1,column:c,row:r,width:Math.min(REGION_SIZE,size-c),height:Math.min(REGION_SIZE,size-r)});
    }
    return out;
  }
  function edges(column,row,size){
    if(!Number.isInteger(column)||!Number.isInteger(row)||column<0||row<0||column>=size||row>=size)return [];
    const result=[];
    if(row%REGION_SIZE===0)result.push([[0,0],[1,0]]);
    if((row+1)%REGION_SIZE===0||row===size-1)result.push([[0,1],[1,1]]);
    if(column%REGION_SIZE===0)result.push([[0,0],[0,1]]);
    if((column+1)%REGION_SIZE===0||column===size-1)result.push([[1,0],[1,1]]);
    return result;
  }
  return Object.freeze({REGION_SIZE,regions,edges});
});
