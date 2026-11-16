#!/usr/bin/env python3
"""Export four joint-position guides, not character art or runtime frames."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[2]
RIG=ROOT/'preview/assets/character-redraw/run-rig'
data=json.loads((RIG/'joints.json').read_text())
OUT=RIG/'keypose-guides';OUT.mkdir(parents=True,exist_ok=True)
font=ImageFont.load_default()
for view in ('front','back'):
    character=Image.open(ROOT/'preview/assets'/('main-character.png' if view=='front' else 'main-character-back.png')).convert('RGBA')
    for label,index in (('a',3),('b',9)):
        canvas=Image.new('RGBA',(512,900),(244,247,241,255))
        faded=character.copy();faded.putalpha(faded.getchannel('A').point(lambda a:round(a*.27)))
        canvas.alpha_composite(faded,(40,45))
        d=ImageDraw.Draw(canvas)
        for side,color in [('far',(22,102,156,255)),('near',(171,66,42,255))]:
            limb=data['views'][view][index][side]
            for chain in [('shoulder','elbow','wrist'),('hip','knee','ankle')]:
                points=[(40+2*limb[n][0],45+2*limb[n][1]) for n in chain]
                d.line(points,fill=color,width=6,joint='curve')
                for x,y in points:d.ellipse((x-6,y-6,x+6,y+6),fill=color)
        d.text((20,12),f'GUIDE ONLY - {view.upper()} - STEP {label.upper()}   frame {index+1}/12',font=font,fill=(25,39,28,255))
        d.text((20,862),'RED: near limb    BLUE: far limb    NOT FINAL ART',font=font,fill=(25,39,28,255))
        canvas.convert('RGB').save(OUT/f'{view}-{label}.png',optimize=True)
