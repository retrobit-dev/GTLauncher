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
        if frame=='lowering':
            source=ART/f'idle-sitting-lowering-{view}.png'
            # The study clips a few boot pixels; replace the boots below with the
            # undamaged standing art rather than retaining the cropped edge.
            original=Image.open(source).convert('RGB')
            pad=Image.new('RGB',(original.width+4,original.height+4),original.getpixel((0,0)))
            pad.paste(original,(2,2));tmp=ART/f'.lowering-{view}-padded.png';pad.save(tmp)
            try: im,box=extract(tmp)
            finally: tmp.unlink()
            scale=min(1,760/im.height,1150/im.width)
            im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
            canvas=Image.new('RGBA',(1280,800));canvas.alpha_composite(im,((1280-im.width)//2,800-im.height))
        else:
            file={'front':{'sitting':'sit-front-hands-corrected.png','nearly':'sit-redraw-near-front.png'},
                  'back':{'sitting':'sit-redraw-final-back.png','nearly':'sit-redraw-near-back.png'}}[view][frame]
            if view=='front':
                # Reposition the near supporting arm while keeping the far hand
                # behind it. Both poses use this same patch, avoiding arm jumps.
                original=Image.open(ART/'sit-front-hands-corrected.png').convert('RGB')
                arm=Image.open(ART/'sit-front-hand-back-study.png').convert('RGB')
                pose=Image.open(ART/file).convert('RGB')
                if pose.size!=original.size or arm.size!=original.size:raise ValueError('front study coordinates differ')
                # Recover the previously visible far hand only through the new
                # foreground arm's background: it stays correctly occluded.
                oldpx=original.load();newpx=arm.load()
                def background(rgb):
                    r,g,b=rgb
                    return r>210 and g>195 and b>175 and abs(r-g)<35 and abs(g-b)<40
                for yy in range(635,685):
                    for xx in range(840,930):
                        if background(newpx[xx,yy]) and not background(oldpx[xx,yy]):
                            newpx[xx,yy]=oldpx[xx,yy]
                mask=Image.new('L',pose.size)
                ImageDraw.Draw(mask).polygon([(740,420),(900,420),(1020,520),(1020,760),(740,760)],fill=255)
                pose.paste(arm,(0,0),mask);tmp=ART/'.front-arm-corrected.png';pose.save(tmp)
                try: im,box=extract(tmp)
                finally: tmp.unlink()
            else:
                im,box=extract(ART/file)
            # Both redrawn studies per side use the same 1306x816 coordinates.
            # Place them at native size without scaling either the body or legs.
            dx=-13;dy=6 if view=='front' else 39
            canvas=Image.new('RGBA',(1280,800));canvas.alpha_composite(im,(box[0]+dx,box[1]+dy))
            # The actual standing head, cap and hair remain pixel-identical.
            head=standing.copy();head_mask=Image.new('L',head.size);draw=ImageDraw.Draw(head_mask)
            if view=='front':
                draw.polygon([(0,0),(432,0),(432,280),(390,280),(375,310),(350,332),(310,345),(250,355),(190,355),(130,346),(80,323),(40,290),(0,280)],fill=255)
                area=[(452,0),(862,0),(862,336),(832,356),(802,376),(767,390),(717,401),(637,411),(557,406),(507,386),(477,361),(452,326)];pos=(447,68)
            else:
                draw.polygon([(0,0),(432,0),(432,265),(390,265),(375,320),(335,350),(250,360),(140,360),(45,330),(0,290)],fill=255)
                area=[(406,0),(776,0),(776,384),(746,419),(706,439),(661,449),(546,449),(466,419),(421,379),(406,339)];pos=(386,94)
            head.putalpha(ImageChops.multiply(head.getchannel('A'),head_mask))
            clear=Image.new('L',canvas.size);clean=ImageDraw.Draw(clear);clean.polygon(area,fill=255)
            clean.rectangle((pos[0],pos[1],pos[0]+431,pos[1]+189),fill=255)
            canvas.paste((0,0,0,0),(0,0),clear);canvas.alpha_composite(head,pos)
        if frame=='lowering':
            padded=Image.new('RGBA',(1280,800));padded.alpha_composite(standing,(424,0))
            join=Image.new('L',(1280,800));join.putdata([round(255*min(1,max(0,(y-682)/30))) for y in range(800) for _ in range(1280)])
            canvas=Image.composite(padded,canvas,join)
        if frame in ('nearly','sitting'):
            # Small enclosed background pockets between arms/legs cannot be reached
            # by border flood fill. Limit cleanup to their known transparent regions.
            region=(830,625,848,705) if view=='front' else (809,675,831,699)
            px=canvas.load()
            for yy in range(region[1],region[3]):
                for xx in range(region[0],region[2]):
                    r,g,b,a=px[xx,yy]
                    if a and r>210 and g>200 and b>180 and max(r,g,b)-min(r,g,b)<45:
                        px[xx,yy]=(0,0,0,0)
        suffix={'lowering':'sit-lowering','nearly':'sit-nearly','sitting':'sitting'}[frame]
        name=f'player-idle-{view}-{suffix}.png';dest=ROOT/'preview/assets'/name
        canvas.save(dest,optimize=True)
        (ROOT/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).write_bytes(dest.read_bytes())
        print(view,frame,box,canvas.getbbox())
