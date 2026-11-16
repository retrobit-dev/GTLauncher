#!/usr/bin/env python3
"""Make transparent, centred review sprites; requires Pillow. Not game assets yet."""
from collections import deque
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'preview/assets/character-redraw'
for view in ('front', 'back'):
    image = Image.open(ROOT / f'character-new-{view}2.png').convert('RGBA')
    w, h = image.size
    pix = image.load()
    bg = pix[0, 0][:3]
    seen = bytearray(w*h)
    queue = deque()
    def visit(x, y):
        index = y*w+x
        if seen[index]: return
        seen[index] = 1
        if max(abs(pix[x,y][i]-bg[i]) for i in range(3)) <= 38:
            queue.append((x,y))
    for x in range(w): visit(x,0); visit(x,h-1)
    for y in range(h): visit(0,y); visit(w-1,y)
    while queue:
        x,y=queue.popleft()
        pix[x,y]=(0,0,0,0)
        if x: visit(x-1,y)
        if x+1<w: visit(x+1,y)
        if y: visit(x,y-1)
        if y+1<h: visit(x,y+1)
    box=image.getbbox()
    if box is None or box[0] == 0 or box[1] == 0 or box[2] == w or box[3] == h:
        raise ValueError(f'Background removal or full-body framing failed for {view}: {box}')
    image=image.crop(box)
    scale=min(210/image.width,392/image.height)
    image=image.resize((round(image.width*scale),round(image.height*scale)), Image.Resampling.LANCZOS)
    out=Image.new('RGBA',(216,400))
    out.alpha_composite(image,((216-image.width)//2,398-image.height))
    out.save(ROOT / f'draft-{view}.png')
    print(view, 'source bounds',box,'normalized',image.size)
