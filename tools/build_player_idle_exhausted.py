#!/usr/bin/env python3
"""Extract the new exhausted key poses; preserve the original sprites and foot anchor."""
from pathlib import Path
from PIL import Image, ImageOps
ROOT=Path(__file__).resolve().parents[1]
specs=[('front','transition',.94),('front','concept',.86),('back','transition',.94),('back','concept',.86)]
for view,pose,relative_height in specs:
    im=Image.open(ROOT/f'preview/assets/character-redraw/idle-exhausted-{view}-{pose}.png').convert('RGBA')
    pixels=im.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a=pixels[x,y]
            bg=(r>120 and b>90 and g<85 and r>g*1.8 and b>g*1.5) if view=='front' else (g>120 and r<115 and b<145 and g>r*1.7 and g>b*1.35)
            if bg: pixels[x,y]=(0,0,0,0)
    box=im.getbbox()
    if not box or box[0]==0 or box[2]==im.width:raise ValueError(f'invalid crop {view}/{pose}: {box}')
    im=im.crop(box)
    if view=='back':im=ImageOps.mirror(im) # match the existing back-facing reference
    height=round(800*relative_height);scale=min(416/im.width,height/im.height)
    im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(432,800));canvas.alpha_composite(im,((432-im.width)//2,798-im.height))
    name=f'player-idle-{view}-{pose}.png';dest=ROOT/'preview/assets'/name;canvas.save(dest,optimize=True)
    (ROOT/f'app/src/main/res/drawable-nodpi/{name.replace("-","_")}').write_bytes(dest.read_bytes())
    print(name,box,canvas.getbbox())
