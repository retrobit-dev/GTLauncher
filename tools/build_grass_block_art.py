#!/usr/bin/env python3
"""Adapt the selected grass block motif to the existing sparse island sheet."""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'preview/assets/character-redraw/grass-block-concept.png').convert('RGB')
# Center tuft from the top surface; transparent matting removes its sage background.
crop=source.crop((865,460,970,545))
bg=(171,196,165)
layer=Image.new('RGBA',crop.size)
for y in range(crop.height):
    for x in range(crop.width):
        r,g,b=crop.getpixel((x,y))
        diff=max(abs(r-bg[0]),abs(g-bg[1]),abs(b-bg[2]))
        alpha=max(0,min(255,(diff-8)*14))
        if alpha: layer.putpixel((x,y),(r,g,b,alpha))
box=layer.getbbox()
if not box: raise ValueError('No tuft found in concept')
tuft=layer.crop(box).resize((24,24),Image.Resampling.LANCZOS)
patterns=[[],[(110,72),(202,86)],[(152,56),(200,88),(110,92)],[(126,62),(176,94)]]
sheet=Image.new('RGBA',(1280,160))
for frame,points in enumerate(patterns):
    for i,(x,y) in enumerate(points):
        sprite=tuft.transpose(Image.Transpose.FLIP_LEFT_RIGHT) if (frame+i)%2 else tuft
        sheet.alpha_composite(sprite,(frame*320+x-12,y-12))
for path in (root/'preview/assets/island-grass-detail.png',root/'app/src/main/res/drawable-nodpi/island_grass_detail.png'):
    sheet.save(path)
# A transparent thumbnail of the selected standalone block for the preview editor.
# The near-neutral background is removed; the green block remains intact.
block=source.crop((300,170,1560,1150)); thumb=Image.new('RGBA',block.size)
for y in range(block.height):
    for x in range(block.width):
        r,g,b=block.getpixel((x,y))
        chroma=max(r,g,b)-min(r,g,b)
        darkness=230-max(r,g,b)
        alpha=max(0,min(255,(max(chroma,darkness)-12)*14))
        if alpha: thumb.putpixel((x,y),(r,g,b,alpha))
box=thumb.getbbox()
if not box: raise ValueError('No block found in concept')
thumb=thumb.crop(box); thumb.thumbnail((256,256),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(256,256));canvas.alpha_composite(thumb,((256-thumb.width)//2,(256-thumb.height)//2))
canvas.save(root/'preview/assets/hud/grass-block.png')
