#!/usr/bin/env python3
"""Prepare the selected tuft as a transparent sprite without replacing older artwork."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract

root=Path(__file__).resolve().parents[1]
art,_=extract(root/'preview/assets/character-redraw/wild-grass-new-concept.png')
# The previous Android grass has a 2:1 sprite box. Centre the original proportions
# in a box of the same shape rather than stretching the vertical blades sideways.
art.thumbnail((480,256),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(512,256))
canvas.alpha_composite(art,((512-art.width)//2,256-art.height))
out=root/'preview/assets/wild-grass-selected.png'
canvas.save(out,optimize=True)
(root/'app/src/main/res/drawable-nodpi/wild_grass_selected.png').write_bytes(out.read_bytes())
