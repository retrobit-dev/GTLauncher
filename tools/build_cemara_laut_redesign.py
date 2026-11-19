#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
root=Path(__file__).resolve().parents[1]
art,_=extract(root/'preview/assets/character-redraw/cemara-laut-redesign.png')
# Remove disconnected cream gaps between the fine branches too.
pixels=art.load()
for y in range(art.height):
    for x in range(art.width):
        r,g,b,a=pixels[x,y]
        if a and r>=190 and g>=180 and b>=155 and abs(r-g)<40 and abs(g-b)<45:pixels[x,y]=(0,0,0,0)
art.thumbnail((480,490),Image.Resampling.LANCZOS)
out=Image.new('RGBA',(512,490));out.alpha_composite(art,((512-art.width)//2,490-art.height))
path=root/'preview/assets/cemara-laut-redesign.png';out.save(path,optimize=True)
(root/'app/src/main/res/drawable-nodpi/cemara_laut_redesign.png').write_bytes(path.read_bytes())
