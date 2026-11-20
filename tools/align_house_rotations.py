#!/usr/bin/env python3
"""Center both house views on the same front foundation/ground-contact point.

The existing transparent sources are kept as-is. Their lower stone corner is
approximately at x=275/274 on row 485 (the plants extend below the stonework).
Centering that point makes mirroring preserve its world position.
"""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
for source, output, contact_x in [
    ('simple-house.png','simple-house-aligned.png',275),
    ('simple-house-back.png','simple-house-back-aligned.png',274),
]:
    img=Image.open(root/'preview/assets'/source).convert('RGBA')
    result=Image.new('RGBA',img.size,(0,0,0,0))
    result.alpha_composite(img,(img.width//2-contact_x,0))
    result.save(root/'preview/assets'/output,optimize=True)
    result.save(root/'app/src/main/res/drawable-nodpi'/output.replace('-','_'),optimize=True)
