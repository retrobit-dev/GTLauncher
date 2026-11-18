#!/usr/bin/env python3
"""Prepare isolated sitting studies in the same coordinate system as idle bow."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
ROOT=Path(__file__).resolve().parents[1]
for view,source in [('front','idle-sitting-front-study.png'),('back','idle-sitting-back-side.png')]:
    im,box=extract(ROOT/'preview/assets/character-redraw'/source)
    scale=min(488/im.width,650/im.height)
    im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(512,800));canvas.alpha_composite(im,((512-im.width)//2,800-im.height))
    name=f'player-idle-{view}-sitting.png';dest=ROOT/'preview/assets'/name
    canvas.save(dest,optimize=True)
    (ROOT/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).write_bytes(dest.read_bytes())
    print(view,box,canvas.getbbox())
