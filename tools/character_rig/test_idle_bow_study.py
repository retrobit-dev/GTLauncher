#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[2]
folder=root/'preview/assets/character-redraw/idle-bow-manual-study'
for side in ('front','back'):
 source=Image.open(root/('preview/assets/main-character.png' if side=='front' else 'preview/assets/main-character-back.png')).convert('RGBA')
 for part in ('head','torso','near_hand','lower_body'):
  study=Image.open(folder/f'{side}-{part}.png').convert('RGBA')
  assert study.size==source.size==(432,800)
  assert study.getchannel('A').getbbox()
  for (r,g,b,a),(sr,sg,sb,sa) in zip(study.getdata(),source.getdata()):
   if a: assert (r,g,b,a)==(sr,sg,sb,sa)
print('Manual idle study uses original source pixels and keeps source coordinates: OK')
