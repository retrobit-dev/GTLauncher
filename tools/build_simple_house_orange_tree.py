#!/usr/bin/env python3
from PIL import Image, ImageDraw
from pathlib import Path
root=Path(__file__).resolve().parents[1]
items=[('house-player-simple-concept.png','simple-house.png'),('house-player-simple-back-concept.png','simple-house-back.png'),('orange-tree-concept.png','orange-tree.png')]
for source,name in items:
    image=Image.open(root/'preview/assets/character-redraw'/source).convert('RGB')
    mask=Image.new('L',image.size)
    mask.putdata([255 if min(p)>220 and max(p)-min(p)<14 else 0 for p in image.getdata()])
    ImageDraw.floodfill(mask,(0,0),128,thresh=0)
    alpha=mask.point(lambda x:0 if x==128 else 255)
    image.putalpha(alpha); image=image.crop(alpha.getbbox()); image.thumbnail((504,482),Image.Resampling.LANCZOS)
    out=Image.new('RGBA',(512,490));out.alpha_composite(image,((512-image.width)//2,490-image.height))
    out.save(root/'preview/assets'/name,optimize=True)
    out.save(root/'app/src/main/res/drawable-nodpi'/name.replace('-','_'),optimize=True)
