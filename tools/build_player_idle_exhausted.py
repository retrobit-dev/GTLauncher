#!/usr/bin/env python3
"""Extract the new exhausted key poses; preserve the original sprites and foot anchor."""
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
specs=[('front','transition'),('front','concept'),('back','concept'),('back','transition')]
back_occluded=None
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
    if view=='back':
        mask=Image.new('L',canvas.size);draw=ImageDraw.Draw(mask)
        if pose=='concept':
            # Far hand is behind the body in this rear view. Rebuild the covered fabric
            # using unoccluded pixels from the original player rather than painting skin.
            draw.polygon([(126,489),(192,487),(194,526),(199,540),(213,557),(220,584),(217,612),(204,628),(147,626),(137,618),(137,574),(129,550),(125,510)],fill=255)
            source=Image.open(ROOT/'preview/assets/main-character.png').convert('RGBA')
            patch=Image.new('RGBA',canvas.size);patch.paste(source.crop((0,60,432,800)),(0,0))
            canvas=Image.composite(patch,canvas,mask)
            back_occluded=canvas.copy()
        else:
            # Transition uses the matching already-repaired trousers behind the arm.
            draw.polygon([(162,493),(215,466),(232,475),(248,503),(265,518),(282,535),(292,554),(294,577),(285,599),(270,612),(217,614),(204,599),(202,565),(190,548),(178,527)],fill=255)
            canvas=Image.composite(back_occluded,canvas,mask)
            pixels=canvas.load()
            def skin(v):
                r,g,b,a=v
                return a and r>150 and r>g*1.25 and r>b*1.25 and g>65
            # Clean tiny skin/outline remnants along the overlap using adjacent fabric pixels.
            for y in range(510,615):
                for x in range(165,298):
                    if skin(pixels[x,y]):
                        for k in range(1,100):
                            if x-k>=80 and not skin(pixels[x-k,y]) and pixels[x-k,y][3]>200:
                                pixels[x,y]=pixels[x-k,y];break
            seam=Image.new('L',canvas.size)
            ImageDraw.Draw(seam).polygon([(280,536),(292,544),(298,558),(298,584),(291,600),(280,598)],fill=255)
            ms=seam.load()
            for y in range(530,605):
                for x in range(275,300):
                    if ms[x,y]:pixels[x,y]=pixels[x-18,y]
    name=f'player-idle-{view}-{pose}.png';dest=ROOT/'preview/assets'/name;canvas.save(dest,optimize=True)
    (ROOT/f'app/src/main/res/drawable-nodpi/{name.replace("-","_")}').write_bytes(dest.read_bytes())
    print(name,box,canvas.getbbox())
