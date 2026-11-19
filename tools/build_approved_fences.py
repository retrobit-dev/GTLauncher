#!/usr/bin/env python3
"""Isolate the approved fence concepts, excluding their illustrated terrain/shadows."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops
ROOT=Path(__file__).resolve().parents[1]
# Coordinates are in a 768x512 reference view, following the outer fence silhouette.
shapes={
 'wood':('fence-new-concept.png',[(187,370),(183,188),(275,184),(275,223),(496,162),(496,105),(582,105),(580,291),(571,301),(548,305),(518,300),(515,279),(273,343),(270,371),(253,380),(215,381),(190,374)]),
 'stone':('fence-stone-concept.png',[(175,380),(170,105),(575,105),(575,297),(566,305),(545,307),(524,303),(260,379),(259,387),(235,393),(205,392),(177,381)]),
}
for name,(source,poly) in shapes.items():
    im=Image.open(ROOT/'preview/assets/character-redraw'/source).convert('RGBA')
    mask=Image.new('L',im.size);ImageDraw.Draw(mask).polygon([(round(x*im.width/768),round(y*im.height/512)) for x,y in poly],fill=255)
    # The conceptual wood image already has alpha; stone has a cream background.
    if name=='stone':
        # Remove only background connected to the image border: pale stones remain intact.
        from collections import deque
        alpha=Image.new('L',im.size,255);out=alpha.load();px=im.load();seen=bytearray(im.width*im.height);q=deque()
        def visit(x,y):
            i=y*im.width+x
            if seen[i]:return
            seen[i]=1;r,g,b,_=px[x,y]
            if r>220 and g>190 and b>160 and r-g>8 and g-b>8:q.append((x,y))
        for x in range(im.width):visit(x,0);visit(x,im.height-1)
        for y in range(im.height):visit(0,y);visit(im.width-1,y)
        while q:
            x,y=q.popleft();out[x,y]=0
            if x:visit(x-1,y)
            if x+1<im.width:visit(x+1,y)
            if y:visit(x,y-1)
            if y+1<im.height:visit(x,y+1)
        mask=ImageChops.multiply(mask,alpha)
    else:
        mask=ImageChops.multiply(mask,im.getchannel('A'))
        pix=im.load();m=mask.load()
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,_=pix[x,y]
                if g>r*.9 and b<g*.75 and r<190:m[x,y]=0
    im.putalpha(mask);box=im.getbbox();im=im.crop(box);im.thumbnail((480,390),Image.Resampling.LANCZOS)
    target=Image.new('RGBA',(512,400));target.alpha_composite(im,((512-im.width)//2,400-im.height))
    path=ROOT/'preview/assets'/f'fence-approved-{name}.png';target.save(path,optimize=True)
    (ROOT/'app/src/main/res/drawable-nodpi'/f'fence_approved_{name}.png').write_bytes(path.read_bytes())
