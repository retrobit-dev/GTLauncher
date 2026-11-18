#!/usr/bin/env python3
from pathlib import Path
import json
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'preview/assets/character-redraw/sitting-reference-parts'
metadata=json.loads((OUT/'landmarks.json').read_text())
assert metadata['source_size']==[432,800]
for view in ('front','back'):
 source=Image.open(ROOT/('preview/assets/main-character.png' if view=='front' else 'preview/assets/main-character-back.png')).convert('RGBA')
 for part in ('head','torso','far_leg','near_leg'):
  layer=Image.open(OUT/f'{view}-{part}.png').convert('RGBA')
  assert layer.size==source.size
  for a,b in zip(layer.getdata(),source.getdata()):
   if a[3]: assert a==b, f'{view}/{part} changes source pixels'
print('Sitting reference layers retain original dimensions and source pixels: OK')
