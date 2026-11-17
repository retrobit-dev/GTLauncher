#!/usr/bin/env python3
from pathlib import Path
from PIL import Image,ImageChops
r=Path(__file__).resolve().parents[1]
a=Image.open(r/'preview/assets/orange-tree-pixel.png').convert('RGBA')
b=Image.open(r/'app/src/main/res/drawable-nodpi/orange_tree_pixel.png').convert('RGBA')
assert a.size==(512,490) and ImageChops.difference(a,b).getbbox() is None
alpha=a.getchannel('A');assert alpha.getpixel((0,0))==0 and alpha.getbbox() is not None
xs=[x for x in range(512) if alpha.getpixel((x,480))>0]
assert abs((min(xs)+max(xs))/2-256)<=1, 'root must be anchored near the bottom centre'
assert (r/'preview/assets/orange-tree.png').exists(), 'old artwork must be preserved'
assert 'orange-tree-pixel.png' in (r/'preview/app.js').read_text()
assert 'orange-tree-pixel.png' in (r/'preview/index.html').read_text()
assert 'R.drawable.orange_tree_pixel' in (r/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
print('Pixel orange tree transparency, anchoring, artwork parity and wiring: OK')
