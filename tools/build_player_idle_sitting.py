#!/usr/bin/env python3
"""Prepare seated and intermediate sprites, retaining original sprite pixel scale."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'preview/assets/character-redraw'
for view in ('front','back'):
    standing=Image.open(ROOT/('preview/assets/main-character.png' if view=='front' else 'preview/assets/main-character-back.png')).convert('RGBA')
    for frame in ('lowering','nearly','sitting'):
        source=ART/f'idle-sitting-{("guided" if frame=="sitting" else frame)}-{view}.png'
        # The lower edge of the crouch study cuts a few pixels of boot. Pad to extract,
        # then replace the boots and ankle with the uncut standing artwork below.
        if frame=='lowering':
            original=Image.open(source).convert('RGB')
            pad=Image.new('RGB',(original.width+4,original.height+4),original.getpixel((0,0)))
            pad.paste(original,(2,2));tmp=ART/f'.lowering-{view}-padded.png';pad.save(tmp)
            try: im,box=extract(tmp)
            finally: tmp.unlink()
        else: im,box=extract(source)
        height=760 if frame=='lowering' else 700
        scale=min(1,height/im.height,1150/im.width)
        im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
        canvas=Image.new('RGBA',(1280,800))
        # Keep the torso over the block while permitting extended legs beyond it.
        anchor=.5 if frame=='lowering' else (.68 if view=='front' else .36)
        x=max(0,min(1280-im.width,round(640-anchor*im.width)))
        canvas.alpha_composite(im,(x,800-im.height))
        if frame=='lowering':
            padded=Image.new('RGBA',(1280,800));padded.alpha_composite(standing,(424,0))
            join=Image.new('L',(1280,800));join.putdata([round(255*min(1,max(0,(y-682)/30))) for y in range(800) for _ in range(1280)])
            canvas=Image.composite(padded,canvas,join)
        suffix={'lowering':'sit-lowering','nearly':'sit-nearly','sitting':'sitting'}[frame]
        name=f'player-idle-{view}-{suffix}.png';dest=ROOT/'preview/assets'/name
        canvas.save(dest,optimize=True)
        (ROOT/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).write_bytes(dest.read_bytes())
        print(view,frame,box,canvas.getbbox())
