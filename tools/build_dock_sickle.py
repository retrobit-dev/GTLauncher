#!/usr/bin/env python3
"""Prepare the selected pixel-art sickle for both dock renderers (requires Pillow)."""
from collections import deque
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'preview/assets/character-redraw/sickle-reference-concept.png'
im=Image.open(source).convert('RGBA');w,h=im.size;p=im.load();seen=bytearray(w*h);q=deque()
def visit(x,y):
    i=y*w+x
    if seen[i]:return
    seen[i]=1
    r,g,b,a=p[x,y]
    if a<8 or min(r,g,b)>=210:q.append((x,y))
for x in range(w):visit(x,0);visit(x,h-1)
for y in range(h):visit(0,y);visit(w-1,y)
while q:
    x,y=q.popleft();p[x,y]=(0,0,0,0)
    if x:visit(x-1,y)
    if x+1<w:visit(x+1,y)
    if y:visit(x,y-1)
    if y+1<h:visit(x,y+1)
box=im.getchannel('A').getbbox()
if not box or box[0]==0 or box[1]==0 or box[2]==w or box[3]==h:raise ValueError(f'background extraction failed: {box}')
im=im.crop(box)
# Keep the same artwork, tilted like the other dock tools.
im=im.rotate(-40, resample=Image.Resampling.BICUBIC, expand=True)
rotated_box=im.getchannel('A').getbbox()
im=im.crop(rotated_box)
scale=min(464/im.width,464/im.height)
im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(512,512));canvas.alpha_composite(im,((512-im.width)//2,(512-im.height)//2))
preview=ROOT/'preview/assets/hud/tools/sickle-selected.png';canvas.save(preview,optimize=True)
(ROOT/'app/src/main/res/drawable-nodpi/dock_tool_sickle.png').write_bytes(preview.read_bytes())
print('Extracted',box,'dock artwork',im.size)
