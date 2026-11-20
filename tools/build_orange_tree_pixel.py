#!/usr/bin/env python3
"""Cut out the approved pixel-art concept; keep the previous tree assets intact."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
image=Image.open(root/'preview/assets/character-redraw/orange-tree-pixel-proportional-concept.png').convert('RGB')
mask=Image.new('L',image.size)
mask.putdata([255 if min(p)>220 and max(p)-min(p)<14 else 0 for p in image.getdata()])
ImageDraw.floodfill(mask,(0,0),128,thresh=0)
alpha=mask.point(lambda v:0 if v==128 else 255)
image.putalpha(alpha);image=image.crop(alpha.getbbox());image.thumbnail((504,482),Image.Resampling.LANCZOS)
out=Image.new('RGBA',(512,490));out.alpha_composite(image,((512-image.width)//2,490-image.height))
# Center the two lower root tips around the ground anchor, including mirrored views.
row=480; alpha=out.getchannel('A');xs=[x for x in range(512) if alpha.getpixel((x,row))>0]
shift=256-round((min(xs)+max(xs))/2)
anchored=Image.new('RGBA',out.size);anchored.alpha_composite(out,(shift,0));out=anchored
for path in [root/'preview/assets/orange-tree-pixel.png',root/'app/src/main/res/drawable-nodpi/orange_tree_pixel.png']:
    out.save(path,optimize=True)
