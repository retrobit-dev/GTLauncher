#!/usr/bin/env python3
"""Extract the new exhausted key poses; preserve the original sprites and foot anchor."""
from pathlib import Path
from PIL import Image, ImageOps
ROOT=Path(__file__).resolve().parents[1]
specs=[('front','transition'),('front','concept'),('back','transition'),('back','concept')]
for view,pose in specs:
    im=Image.open(ROOT/f'preview/assets/character-redraw/idle-exhausted-{view}-{pose}.png').convert('RGBA')
    pixels=im.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a=pixels[x,y]
            bg=(r>120 and b>90 and g<85 and r>g*1.8 and b>g*1.5) if view=='front' else (g>120 and r<115 and b<145 and g>r*1.7 and g>b*1.35)
            if bg: pixels[x,y]=(0,0,0,0)
    box=im.getbbox()
    if not box or box[0]==0 or box[2]==im.width:raise ValueError(f'invalid crop {view}/{pose}: {box}')
    im=im.crop(box)
    if view=='back':im=ImageOps.mirror(im) # match the existing back-facing reference
    # Use one fixed fit for all poses. Lower heads come from the artwork, never by shrinking the whole player.
    scale=min(400/im.width,792/im.height)
    im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(432,800))
    standing=Image.open(ROOT/('preview/assets/main-character-back.png' if view=='back' else 'preview/assets/main-character.png')).convert('RGBA')
    # Align the centre of the boots to the standing pose, not the centre of the hat or backpack.
    target=standing.getchannel('A').crop((0,760,432,800)).getbbox()
    feet=im.getchannel('A').crop((0,im.height-40,im.width,im.height)).getbbox()
    x=round((target[0]+target[2]-feet[0]-feet[2])/2)
    x=max(0,min(432-im.width,x))
    canvas.alpha_composite(im,(x,800-im.height))
    # Reuse the standing boots verbatim, feathering only the ankle join. This keeps the
    # foot size and placement identical through all frames without redrawing the player.
    join=Image.new('L',(432,800))
    join.putdata([round(255*min(1,max(0,(y-682)/30))) for y in range(800) for _ in range(432)])
    canvas=Image.composite(standing,canvas,join)
    name=f'player-idle-{view}-{pose}.png';dest=ROOT/'preview/assets'/name;canvas.save(dest,optimize=True)
    (ROOT/f'app/src/main/res/drawable-nodpi/{name.replace("-","_")}').write_bytes(dest.read_bytes())
    print(name,box,canvas.getbbox())
