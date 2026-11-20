#!/usr/bin/env python3
"""Extract the three selected status icons, keeping only the main opaque shape."""
from pathlib import Path
from collections import deque
import colorsys
from PIL import Image
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'preview/assets/character-redraw/status-bars-redesign-concept.png').convert('RGBA')
for name,top,bottom in [('health',380,510),('stamina',510,630),('food',630,750),('water',750,870)]:
    img=source.crop((320,top,450,bottom))
    w,h=img.size
    # The generated artwork has faint stray pixels; retain the largest foreground component.
    px=img.load(); mask={(x,y) for y in range(h) for x in range(w) if px[x,y][3]>=128}
    components=[]
    while mask:
        seed=mask.pop(); q=deque([seed]); component=[seed]
        while q:
            x,y=q.popleft()
            for n in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
                if n in mask: mask.remove(n);q.append(n);component.append(n)
        components.append(component)
    keep=set(max(components,key=len)); out=Image.new('RGBA',img.size)
    for x,y in keep:
        r,g,b,alpha=px[x,y]
        hue,sat,val=colorsys.rgb_to_hsv(r/255,g/255,b/255)
        # Tint the colored body toward the established HUD palette, while
        # preserving the dark outlines and pale highlights from the concept.
        targets={'health':('#c94e43'), 'stamina':('#e1a52e'), 'food':('#6d9134')}
        if name in targets and sat > .45 and val > .38:
            target=targets[name].lstrip('#')
            rgb=tuple(int(target[i:i+2],16)/255 for i in (0,2,4))
            th,ts,tv=colorsys.rgb_to_hsv(*rgb)
            r,g,b=(round(v*255) for v in colorsys.hsv_to_rgb(th,min(sat,ts),min(val,tv)))
        out.putpixel((x,y),(r,g,b,alpha))
    out=out.crop(out.getbbox()); side=max(out.size); canvas=Image.new('RGBA',(side,side));canvas.alpha_composite(out,((side-out.width)//2,(side-out.height)//2))
    for path in [root/f'preview/assets/hud/status-{name}.png',root/f'app/src/main/res/drawable-nodpi/status_icon_{name}.png']:
        canvas.save(path)
