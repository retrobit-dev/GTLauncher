#!/usr/bin/env python3
"""Extract the three selected status icons, keeping only the main opaque shape."""
from pathlib import Path
from collections import deque
from PIL import Image
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'preview/assets/character-redraw/status-bars-redesign-concept.png').convert('RGBA')
for name,top,bottom in [('health',380,510),('stamina',510,630),('water',750,870)]:
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
    for x,y in keep: out.putpixel((x,y),px[x,y])
    out=out.crop(out.getbbox()); side=max(out.size); canvas=Image.new('RGBA',(side,side));canvas.alpha_composite(out,((side-out.width)//2,(side-out.height)//2))
    for path in [root/f'preview/assets/hud/status-{name}.png',root/f'app/src/main/res/drawable-nodpi/status_icon_{name}.png']:
        canvas.save(path)
