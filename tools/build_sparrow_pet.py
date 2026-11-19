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

# Replace the folded-wing area with the clean raised-wing study. A replacement
# mask (rather than alpha overlay) also removes the old folded feathers.
from PIL import ImageDraw
flight,_=extract(root/'preview/assets/character-redraw/sparrow-flight-clean-study.png')
flight=flight.resize((192,192),Image.Resampling.LANCZOS)
mask=Image.new('L',flight.size)
ImageDraw.Draw(mask).polygon([(96,0),(192,0),(192,145),(150,146),(130,145),(100,136),(86,120),(82,100),(80,83),(95,65)],fill=255)
flap=image.copy();flap.paste(flight,(0,0),mask)
flap_path=root/'preview/assets/pet-sparrow-flap.png';flap.save(flap_path,optimize=True)
(root/'app/src/main/res/drawable-nodpi/pet_sparrow_flap.png').write_bytes(flap_path.read_bytes())

# Rear three-quarter artwork faces right with its tail pointing left, like the rear player.
back,back_box=extract(root/'preview/assets/character-redraw/sparrow-back-facing-corrected.png')
source,source_box=extract(root/'preview/assets/character-redraw/sparrow-back-flight-clean-study.png')
base=Image.new('RGBA',(1024,1024));base.alpha_composite(back,back_box[:2])
flap=Image.new('RGBA',(1024,1024));flap.alpha_composite(source,source_box[:2])
# Retain the resting head and feet to keep their position fixed across frames;
# the body under the raised wing comes from the clean flight art, not the folded wing.
mask=Image.new('L',base.size);draw=ImageDraw.Draw(mask)
draw.polygon([(535,145),(850,145),(850,410),(745,420),(690,390),(625,370),(560,335)],fill=255)
draw.rectangle((0,755,1023,1023),fill=255)
flap.paste(base,(0,0),mask)
for name,frame in [('pet-sparrow-back.png',base),('pet-sparrow-back-flap.png',flap)]:
    frame=frame.crop((80,50,900,900)).resize((192,192),Image.Resampling.LANCZOS)
    out=root/'preview/assets'/name;frame.save(out,optimize=True)
    (root/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).write_bytes(out.read_bytes())
