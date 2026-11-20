#!/usr/bin/env python3
"""Make transparent, centred review sprites; requires Pillow. Not game assets yet."""
from collections import deque
from pathlib import Path
import argparse
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'preview/assets/character-redraw'
parser = argparse.ArgumentParser()
parser.add_argument('--detail', action='store_true', help='process the detailed redraw instead of the original draft')
args = parser.parse_args()
canvas_w, canvas_h, margin = (540, 1000, 10) if args.detail else (216, 400, 4)
for view in ('front', 'back'):
    source = f'hd-{view}-source.png' if args.detail else f'character-new-{view}2.png'
    image = Image.open(ROOT / source).convert('RGBA')
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
    scale=min((canvas_w-2*margin)/image.width,(canvas_h-2*margin)/image.height)
    image=image.resize((round(image.width*scale),round(image.height*scale)), Image.Resampling.LANCZOS)
    out=Image.new('RGBA',(canvas_w,canvas_h))
    out.alpha_composite(image,((canvas_w-image.width)//2,canvas_h-margin//2-image.height))
    name = f'detail-{view}.png' if args.detail else f'draft-{view}.png'
    out.save(ROOT / name)
    print(view, 'source bounds',box,'normalized',image.size)
