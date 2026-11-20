#!/usr/bin/env python3
"""Prepare the lowland jasmine and mulberry artwork, keeping older variants."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
from clean_plant_ground import clean_ground_shadow
root=Path(__file__).resolve().parents[1]
# Concepts reach the lower image edge, so add a temporary pale border for flood fill.
for name in ('jasmine','mulberry'):
    im=Image.open(root/f'preview/assets/character-redraw/bush-{name}-concept.png').convert('RGB')
    canvas=Image.new('RGB',(im.width+32,im.height+32),im.getpixel((0,0)))
    canvas.paste(im,(16,16));temp=root/f'.bush-{name}-extract.png';canvas.save(temp)
    try: art,_=extract(temp)
    finally: temp.unlink()
    art=clean_ground_shadow(art);art.thumbnail((480,480),Image.Resampling.LANCZOS)
    out=Image.new('RGBA',(512,512));out.alpha_composite(art,((512-art.width)//2,512-art.height))
    dest=root/f'preview/assets/bush-{name}.png';out.save(dest,optimize=True)
    (root/f'app/src/main/res/drawable-nodpi/bush_{name}.png').write_bytes(dest.read_bytes())
