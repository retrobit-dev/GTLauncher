#!/usr/bin/env python3
"""Extract the selected concept's wooden panel frame as a reusable nine-slice."""
from pathlib import Path
from PIL import Image, ImageDraw
root = Path(__file__).resolve().parents[1]
source = Image.open(root / 'preview/assets/character-redraw/character-status-panel-concept.png').convert('RGB')
# Frame around the health panel; keep its four corner bolts and timber edges.
box = source.crop((879, 233, 1787, 453))
w, h = box.size
corner = 36
frame = Image.new('RGB', (128, 128), '#f6dca9')
for x, sx in ((0, 0), (92, w-corner)):
    for y, sy in ((0, 0), (92, h-corner)):
        frame.paste(box.crop((sx, sy, sx+corner, sy+corner)), (x,y))
for y, sy in ((0,0),(92,h-corner)):
    frame.paste(box.crop((500,sy,501,sy+corner)).resize((56,corner)),(36,y))
for x, sx in ((0,0),(92,w-corner)):
    frame.paste(box.crop((sx,80,sx+corner,81)).resize((corner,56)),(x,36))
frame=frame.resize((32,32),Image.Resampling.LANCZOS).convert('RGBA')
frame.save(root/'preview/assets/hud/status-frame.png')
# Android nine-patch markers: stretch the central area, with inset content padding.
patch=Image.new('RGBA',(34,34),(0,0,0,0));patch.paste(frame,(1,1))
d=ImageDraw.Draw(patch);d.line((10,0,23,0),fill='black');d.line((0,10,0,23),fill='black');d.line((5,33,28,33),fill='black');d.line((33,5,33,28),fill='black')
patch.save(root/'app/src/main/res/drawable-xhdpi/status_frame.9.png')
