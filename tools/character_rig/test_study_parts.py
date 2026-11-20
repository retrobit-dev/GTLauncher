#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[2]/'preview/assets/character-redraw/run-rig/study-parts'
for view in ('front','back'):
    for part in ('left_arm','right_arm','left_leg','right_leg'):
        im=Image.open(root/f'{view}-{part}.png').convert('RGBA')
        assert im.size==(1024,1024)
        alpha=im.getchannel('A'); box=alpha.getbbox()
        assert box and box[0]>0 and box[1]>0 and box[2]<1024 and box[3]<1024
        assert alpha.getpixel((0,0))==0 and alpha.getpixel((512,100))==0
print('Draft limb studies have transparent backgrounds and consistent source-space canvas: OK')
