#!/usr/bin/env python3
"""Cut out the approved concept and its matching rear view without changing source artwork."""
from PIL import Image, ImageDraw
from pathlib import Path
root = Path(__file__).resolve().parents[1]
for source, name in [('house-redesign-concept.png','player-house.png'), ('house-redesign-back-concept.png','player-house-back.png')]:
    img = Image.open(root/'preview/assets/character-redraw'/source).convert('RGB')
    # Only near-neutral, near-white pixels reachable from the image boundary are background.
    mask = Image.new('L', img.size)
    mask.putdata([255 if min(rgb)>220 and max(rgb)-min(rgb)<14 else 0 for rgb in img.getdata()])
    ImageDraw.floodfill(mask, (0, 0), 128, thresh=0)
    alpha = mask.point(lambda v: 0 if v == 128 else 255)
    img.putalpha(alpha)
    img = img.crop(alpha.getbbox())
    img.thumbnail((504, 482), Image.Resampling.LANCZOS)
    out = Image.new('RGBA', (512,490))
    out.alpha_composite(img, ((512-img.width)//2, 490-img.height))
    out.save(root/'preview/assets'/name, optimize=True)
    out.save(root/'app/src/main/res/drawable-nodpi'/name.replace('-','_'), optimize=True)
