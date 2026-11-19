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

# Composite the raised wing study onto the chosen bird; all other pixels stay fixed.
from PIL import ImageDraw, ImageChops
wing_source,box=extract(root/'preview/assets/character-redraw/sparrow-wing-study.png')
full=Image.new('RGBA',(1024,1024));full.alpha_composite(wing_source,(box[0],box[1]))
mask=Image.new('L',full.size)
ImageDraw.Draw(mask).polygon([(559,383),(676,295),(784,127),(803,70),(830,69),(840,118),(869,103),(890,120),(892,150),(920,148),(933,170),(929,207),(932,229),(910,300),(887,384),(836,474),(763,549),(689,601),(609,584),(570,535)],fill=255)
full.putalpha(ImageChops.multiply(full.getchannel('A'),mask))
wing=full.crop((559,69,934,602)).resize((83,111),Image.Resampling.LANCZOS)
flap=image.copy();flap.alpha_composite(wing,(80,28))
flap_path=root/'preview/assets/pet-sparrow-flap.png';flap.save(flap_path,optimize=True)
(root/'app/src/main/res/drawable-nodpi/pet_sparrow_flap.png').write_bytes(flap_path.read_bytes())
