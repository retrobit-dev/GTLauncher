#!/usr/bin/env python3
"""Extract the chosen original bush; retain the native legacy bush artwork."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
from clean_plant_ground import clean_ground_shadow
root=Path(__file__).resolve().parents[1]
for variant in ('selected','flower','berry'):
    art,_=extract(root/f'preview/assets/character-redraw/{"bush-concept" if variant=="selected" else "bush-"+variant+"-concept"}.png')
    if variant!='selected':art=clean_ground_shadow(art)
    art.thumbnail((480,480),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(512,512))
    canvas.alpha_composite(art,((512-art.width)//2,512-art.height))
    out=root/f'preview/assets/bush-{variant}.png'
    canvas.save(out,optimize=True)
    (root/f'app/src/main/res/drawable-nodpi/{"bush_selected" if variant=="selected" else "bush_"+variant+"_selected"}.png').write_bytes(out.read_bytes())
