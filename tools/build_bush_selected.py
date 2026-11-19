#!/usr/bin/env python3
"""Extract the chosen original bush; retain the native legacy bush artwork."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
root=Path(__file__).resolve().parents[1]
art,_=extract(root/'preview/assets/character-redraw/bush-concept.png')
art.thumbnail((480,480),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(512,512))
canvas.alpha_composite(art,((512-art.width)//2,512-art.height))
out=root/'preview/assets/bush-selected.png'
canvas.save(out,optimize=True)
(root/'app/src/main/res/drawable-nodpi/bush_selected.png').write_bytes(out.read_bytes())
