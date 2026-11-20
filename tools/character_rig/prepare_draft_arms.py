#!/usr/bin/env python3
"""Remove white backgrounds from two experimental detached-arm drawings.

They are fitting studies, not production rig art: elbow articulation, torso
occlusion, opposite limbs and game-scale consistency are not established.
"""
from collections import deque
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]/'preview/assets/character-redraw/run-rig'
for view in ('front','back'):
    image=Image.open(ROOT/'artwork-drafts'/f'{view}-arm.png').convert('RGBA')
    w,h=image.size; pixels=image.load(); seen=bytearray(w*h); q=deque()
    def visit(x,y):
        i=y*w+x
        if seen[i]:return
        seen[i]=1
        r,g,b,a=pixels[x,y]
        if min(r,g,b)>235:q.append((x,y))
    for x in range(w):visit(x,0);visit(x,h-1)
    for y in range(h):visit(0,y);visit(w-1,y)
    while q:
        x,y=q.popleft();pixels[x,y]=(0,0,0,0)
        if x:visit(x-1,y)
        if x+1<w:visit(x+1,y)
        if y:visit(x,y-1)
        if y+1<h:visit(x,y+1)
    box=image.getchannel('A').getbbox()
    if not box or box[0]==0 or box[1]==0 or box[2]==w or box[3]==h:raise ValueError((view,box))
    image.save(ROOT/'artwork-drafts'/f'{view}-arm-transparent.png',optimize=True)
    print(view,box)
