#!/usr/bin/env python3
from pathlib import Path
from PIL import Image, ImageChops
root=Path(__file__).resolve().parents[1]
views=[]
for name in ['simple-house-aligned.png','simple-house-back-aligned.png']:
    path=root/'preview/assets'/name
    im=Image.open(path).convert('RGBA')
    android=Image.open(root/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).convert('RGBA')
    assert ImageChops.difference(im,android).getbbox() is None, 'Android and preview artwork differ'
    alpha=im.getchannel('A'); row=485
    xs=[x for x in range(im.width) if alpha.getpixel((x,row))>128]
    assert xs and abs((min(xs)+max(xs))/2-im.width/2)<1.5, f'{name} contact is not centered'
    views.append(alpha)
    flipped=alpha.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    xs=[x for x in range(im.width) if flipped.getpixel((x,row))>128]
    assert abs((min(xs)+max(xs))/2-im.width/2)<1.5, f'{name} mirrored contact shifts'
for y in [50,100,200,300,380,460,475,485]:
    ranges=[]
    for alpha in views:
        xs=[x for x in range(alpha.width) if alpha.getpixel((x,y))>128]
        ranges.append((min(xs),max(xs)))
    assert max(abs(ranges[0][i]-ranges[1][i]) for i in (0,1))<=5, (y,ranges)
print('Four house orientations: aligned ground contacts, matching extents and platform assets: OK')
