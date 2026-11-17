#!/usr/bin/env python3
"""Extract the selected dirt-block concept as a transparent editor thumbnail."""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'preview/assets/character-redraw/dirt-block-concept.png').convert('RGB')
block=source.crop((350,220,2200,1600)); out=Image.new('RGBA',block.size)
for y in range(block.height):
    for x in range(block.width):
        r,g,b=block.getpixel((x,y))
        chroma=max(r,g,b)-min(r,g,b)
        darkness=230-max(r,g,b)
        alpha=max(0,min(255,(max(chroma,darkness)-12)*14))
        if alpha: out.putpixel((x,y),(r,g,b,alpha))
box=out.getbbox()
if not box: raise ValueError('No dirt block found in concept')
out=out.crop(box);out.thumbnail((256,256),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(256,256));canvas.alpha_composite(out,((256-out.width)//2,(256-out.height)//2))
canvas.save(root/'preview/assets/hud/dirt-block.png')
