#!/usr/bin/env python3
"""Build a scalable frame from the selected character status HUD concept."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'preview/assets/character-redraw/left-hud-new-concept.png').convert('RGB')
box=source.crop((244,216,1518,674)); w,h=box.size; c=40
frame=Image.new('RGB',(144,144),'#f4dfb7')
for x,sx in [(0,0),(104,w-c)]:
    for y,sy in [(0,0),(104,h-c)]: frame.paste(box.crop((sx,sy,sx+c,sy+c)),(x,y))
for y,sy in [(0,0),(104,h-c)]: frame.paste(box.crop((700,sy,701,sy+c)).resize((64,c)),(40,y))
for x,sx in [(0,0),(104,w-c)]: frame.paste(box.crop((sx,220,sx+c,221)).resize((c,64)),(x,40))
frame=frame.resize((32,32),Image.Resampling.LANCZOS).convert('RGBA')
frame.save(root/'preview/assets/hud/character-status-frame.png')
patch=Image.new('RGBA',(34,34),(0,0,0,0));patch.paste(frame,(1,1));d=ImageDraw.Draw(patch)
d.line((10,0,23,0),fill='black');d.line((0,10,0,23),fill='black');d.line((2,33,31,33),fill='black');d.line((33,2,33,31),fill='black')
patch.save(root/'app/src/main/res/drawable-xhdpi/character_status_frame.9.png')
