#!/usr/bin/env python3
"""Extract the selected sparrow concept into matching transparent runtime sprites."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
root=Path(__file__).resolve().parents[1]
image,_=extract(root/'preview/assets/character-redraw/sparrow-pet-concept.png')
image.thumbnail((192,192),Image.Resampling.LANCZOS)
preview=root/'preview/assets/pet-sparrow.png'
image.save(preview,optimize=True)
(root/'app/src/main/res/drawable-nodpi/pet_sparrow.png').write_bytes(preview.read_bytes())
