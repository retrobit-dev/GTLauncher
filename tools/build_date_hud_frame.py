#!/usr/bin/env python3
"""Build a stretchable wooden frame from the chosen date HUD concept."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
im=Image.open(root/'preview/assets/character-redraw/calendar-clock-battery-concept.png').convert('RGB')
box=im.crop((207,269,1327,690)); w,h=box.size; c=42
frame=Image.new('RGB',(160,160),'#f4dfb7')
for x,sx in [(0,0),(118,w-c)]:
 for y,sy in [(0,0),(118,h-c)]: frame.paste(box.crop((sx,sy,sx+c,sy+c)),(x,y))
for y,sy in [(0,0),(118,h-c)]: frame.paste(box.crop((400,sy,401,sy+c)).resize((76,c)),(42,y))
for x,sx in [(0,0),(118,w-c)]: frame.paste(box.crop((sx,210,sx+c,211)).resize((c,76)),(x,42))
frame=frame.resize((32,32),Image.Resampling.LANCZOS).convert('RGBA')
frame.save(root/'preview/assets/hud/date-frame.png')
patch=Image.new('RGBA',(34,34),(0,0,0,0));patch.paste(frame,(1,1));d=ImageDraw.Draw(patch)
d.line((10,0,23,0),fill='black');d.line((0,10,0,23),fill='black');d.line((2,33,31,33),fill='black');d.line((33,2,33,31),fill='black')
patch.save(root/'app/src/main/res/drawable-xhdpi/date_hud_frame.9.png')
