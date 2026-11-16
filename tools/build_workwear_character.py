#!/usr/bin/env python3
"""Extract approved workwear concepts to transparent, consistently sized game sprites."""
from collections import deque
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
for view,source,dest in [('front','outfit-simple-concept.png','main-character.png'),('back','outfit-back-angled-source.png','main-character-back.png')]:
    image=Image.open(ROOT/'preview/assets/character-redraw'/source).convert('RGBA')
    w,h=image.size; pix=image.load(); bg=pix[0,0][:3]; seen=bytearray(w*h); q=deque()
    def visit(x,y):
        i=y*w+x
        if seen[i]: return
        seen[i]=1
        if max(abs(pix[x,y][j]-bg[j]) for j in range(3))<=38: q.append((x,y))
    for x in range(w):visit(x,0);visit(x,h-1)
    for y in range(h):visit(0,y);visit(w-1,y)
    while q:
        x,y=q.popleft();pix[x,y]=(0,0,0,0)
        if x:visit(x-1,y)
        if x+1<w:visit(x+1,y)
        if y:visit(x,y-1)
        if y+1<h:visit(x,y+1)
    if view == "back":
        # Remove white background trapped between the arm and body.
        for y in range(h):
            for x in range(w):
                r,g,b,a=pix[x,y]
                if a and min(r,g,b)>245: pix[x,y]=(0,0,0,0)
    box=image.getbbox()
    if not box or box[0]==0 or box[1]==0 or box[2]==w or box[3]==h: raise ValueError(f'bad extraction {view}: {box}')
    image=image.crop(box)
    # Preserve the existing 432x800 canvas and ground anchor, without distorting proportions.
    scale=min(416/image.width,792/image.height)
    image=image.resize((round(image.width*scale),round(image.height*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(432,800));canvas.alpha_composite(image,((432-image.width)//2,800-image.height))
    path=ROOT/'preview/assets'/dest;canvas.save(path,optimize=True)
    (ROOT/f'app/src/main/res/drawable-nodpi/character_original_uhd_{view}.png').write_bytes(path.read_bytes())
    print(view,box,image.size)
