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
# Repaired artwork stays an isolated study, not silently substituted into runtime.
for side,name in [('front','sit-redraw-repaired-front-v2.png'),('back','sit-redraw-final-back.png')]:
 study=Image.open(ROOT/'preview/assets/character-redraw'/name).convert('RGBA')
 assert study.size==(1306,816)
 assert study.getpixel((0,0))[3]==255
 runtime=Image.open(ROOT/f'preview/assets/player-idle-{side}-sitting.png').convert('RGBA')
 assert runtime.size==(1280,800)
print('Sitting reference layers retain original dimensions and source pixels: OK')
