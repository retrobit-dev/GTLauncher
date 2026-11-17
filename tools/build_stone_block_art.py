#!/usr/bin/env python3
"""Extract the approved stone-block concept for the preview material picker."""
from collections import deque
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
im=Image.open(root/'preview/assets/character-redraw/stone-block-concept.png').convert('RGBA')
w,h=im.size; p=im.load(); seen=bytearray(w*h); q=deque()
def visit(x,y):
    i=y*w+x
    if seen[i]: return
    seen[i]=1
    r,g,b,a=p[x,y]
    if a<8 or (min(r,g,b)>=198 and max(r,g,b)-min(r,g,b)<20): q.append((x,y))
for x in range(w): visit(x,0);visit(x,h-1)
for y in range(h): visit(0,y);visit(w-1,y)
while q:
    x,y=q.popleft();p[x,y]=(0,0,0,0)
    if x: visit(x-1,y)
    if x+1<w: visit(x+1,y)
    if y: visit(x,y-1)
    if y+1<h: visit(x,y+1)
box=im.getchannel('A').getbbox()
if not box or box[0]==0 or box[1]==0 or box[2]==w or box[3]==h: raise ValueError(f'Background extraction failed: {box}')
im=im.crop(box);im.thumbnail((240,240),Image.Resampling.LANCZOS)
out=Image.new('RGBA',(256,256));out.alpha_composite(im,((256-im.width)//2,(256-im.height)//2));out.save(root/'preview/assets/hud/stone-block.png',optimize=True)
print('Stone thumbnail bounds:',box)
