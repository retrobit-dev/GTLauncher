#!/usr/bin/env python3
"""Apply the approved rear-view hand study to the existing seated sprite.

Run after build_player_idle_sitting.py; the rest of the pose stays untouched.
"""
from collections import deque
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

root = Path(__file__).resolve().parents[1]
preview = root / 'preview/assets/player-idle-back-sitting.png'
android = root / 'app/src/main/res/drawable-nodpi/player_idle_back_sitting.png'
original = Image.open(preview).convert('RGBA')
study = Image.open(root / 'artwork/sitting-back-right-hand-options.png').convert('RGBA')
aligned = Image.new('RGBA', original.size)
aligned.paste(study, (-130, 0))
# Remove only the dark background connected to the canvas boundary, preserving
# dark outlines and details inside the character.
pixels = aligned.load()
w, h = aligned.size
queue = deque([(0, 0)])
seen = {(0, 0)}
while queue:
    x, y = queue.popleft()
    if max(pixels[x, y][:3]) > 12:
        continue
    pixels[x, y] = (0, 0, 0, 0)
    for pt in ((x-1, y), (x+1, y), (x, y-1), (x, y+1)):
        if 0 <= pt[0] < w and 0 <= pt[1] < h and pt not in seen:
            seen.add(pt)
            queue.append(pt)
mask = Image.new('L', original.size)
ImageDraw.Draw(mask).polygon([(674,625),(756,616),(776,668),(803,718),
    (875,753),(910,779),(910,799),(710,799),(706,760),(692,704)], fill=255)
mask = mask.filter(ImageFilter.GaussianBlur(2))
result = Image.composite(aligned, original, mask)
result.save(preview, optimize=True)
android.write_bytes(preview.read_bytes())
