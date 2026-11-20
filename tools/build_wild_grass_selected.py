#!/usr/bin/env python3
"""Prepare the selected tuft as a transparent sprite without replacing older artwork."""
from pathlib import Path
import colorsys
from PIL import Image
from build_player_idle_selected import extract
from clean_plant_ground import clean_ground_shadow

root=Path(__file__).resolve().parents[1]
art,_=extract(root/'preview/assets/character-redraw/wild-grass-new-concept.png')
art=clean_ground_shadow(art)
# Match the island's softer sage-green palette while retaining the chosen blade
# shapes, highlights and alpha. Do this before scaling so edges are softened once.
pixels=art.load()
for y in range(art.height):
    for x in range(art.width):
        r,g,b,a=pixels[x,y]
        if not a: continue
        h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
        if g>r and g>b:
            h=max(.29,min(.37,h));s*=.55
            r,g,b=(round(c*255) for c in colorsys.hsv_to_rgb(h,s,v))
        pixels[x,y]=(r,g,b,a)
# The previous Android grass has a 2:1 sprite box. Centre the original proportions
# in a box of the same shape rather than stretching the vertical blades sideways.
art.thumbnail((480,256),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(512,256))
canvas.alpha_composite(art,((512-art.width)//2,256-art.height))
out=root/'preview/assets/wild-grass-selected.png'
canvas.save(out,optimize=True)
(root/'app/src/main/res/drawable-nodpi/wild_grass_selected.png').write_bytes(out.read_bytes())
