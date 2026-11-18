#!/usr/bin/env python3
"""Prepare seated and intermediate sprites, retaining original sprite pixel scale."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops
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
        if frame in ('nearly','sitting'):
            # Reuse the original head and cap at their native pixel size.  These masks
            # are hand placed for the seated drawings; no whole-player scaling is used.
            head=standing.copy();head_mask=Image.new('L',head.size);draw=ImageDraw.Draw(head_mask)
            if view=='front':
                draw.polygon([(0,0),(432,0),(432,280),(390,280),(375,310),(350,332),(310,345),(250,355),(190,355),(130,346),(80,323),(40,290),(0,280)],fill=255)
                area=[(455,0),(865,0),(865,330),(830,350),(805,370),(770,384),(720,395),(640,405),(560,400),(510,380),(480,355),(455,320)];pos=(450,62)
            else:
                draw.polygon([(0,0),(432,0),(432,265),(390,265),(375,320),(335,350),(250,360),(140,360),(45,330),(0,290)],fill=255)
                area=[(410,0),(780,0),(780,345),(750,380),(710,400),(665,410),(550,410),(470,380),(425,340),(410,300)];pos=(390,55)
            head.putalpha(ImageChops.multiply(head.getchannel('A'),head_mask))
            clear=Image.new('L',canvas.size);ImageDraw.Draw(clear).polygon(area,fill=255)
            canvas.paste((0,0,0,0),(0,0),clear);canvas.alpha_composite(head,pos)
        if frame=='lowering':
            padded=Image.new('RGBA',(1280,800));padded.alpha_composite(standing,(424,0))
            join=Image.new('L',(1280,800));join.putdata([round(255*min(1,max(0,(y-682)/30))) for y in range(800) for _ in range(1280)])
            canvas=Image.composite(padded,canvas,join)
        if view=='front' and frame=='nearly':
            # Enclosed cream backdrop between the bent legs is not reachable by the
            # border flood fill. Remove it without touching the actual player.
            px=canvas.load()
            for yy in range(620,700):
                for xx in range(310,378):
                    r,g,b,a=px[xx,yy]
                    if a and r>210 and g>200 and b>180 and max(r,g,b)-min(r,g,b)<45:
                        px[xx,yy]=(0,0,0,0)
        suffix={'lowering':'sit-lowering','nearly':'sit-nearly','sitting':'sitting'}[frame]
        name=f'player-idle-{view}-{suffix}.png';dest=ROOT/'preview/assets'/name
        canvas.save(dest,optimize=True)
        (ROOT/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).write_bytes(dest.read_bytes())
        print(view,frame,box,canvas.getbbox())
