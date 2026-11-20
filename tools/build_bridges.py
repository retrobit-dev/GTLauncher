#!/usr/bin/env python3
"""Prepare selected bridge concepts without altering the source art."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
ROOT=Path(__file__).resolve().parents[1]
for kind in ('wood','stone'):
    source=ROOT/f'preview/assets/character-redraw/bridge-{kind}-three-blocks.png'
    art,_=extract(source)
    # Remove the pale reference background visible through enclosed rail openings.
    pixels=art.load()
    for y in range(art.height):
        for x in range(art.width):
            r,g,b,a=pixels[x,y]
            if a and r>=242 and g>=232 and b>=215 and r-g<20 and g-b<25:pixels[x,y]=(0,0,0,0)
    art.thumbnail((600,400),Image.Resampling.LANCZOS)
    target=ROOT/f'preview/assets/bridge-{kind}.png';art.save(target,optimize=True)
    (ROOT/f'app/src/main/res/drawable-nodpi/bridge_{kind}.png').write_bytes(target.read_bytes())
