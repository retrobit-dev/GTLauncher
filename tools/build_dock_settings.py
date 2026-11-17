#!/usr/bin/env python3
"""Prepare the selected transparent gear for the preview and Android docks."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
image = Image.open(ROOT / 'preview/assets/character-redraw/settings-gear-concept.png').convert('RGBA')
alpha = image.getchannel('A').point(lambda a: 0 if a <= 8 else a)
image.putalpha(alpha)
box = alpha.getbbox()
if not box or box[0] == 0 or box[1] == 0 or box[2] == image.width or box[3] == image.height:
    raise ValueError(f'Invalid transparent artwork bounds: {box}')
image = image.crop(box)
scale = min(464 / image.width, 464 / image.height)
image = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
canvas = Image.new('RGBA', (512, 512))
canvas.alpha_composite(image, ((512-image.width)//2, (512-image.height)//2))
preview = ROOT / 'preview/assets/hud/dock_settings.png'
canvas.save(preview, optimize=True)
(ROOT / 'app/src/main/res/drawable-nodpi/dock_settings.png').write_bytes(preview.read_bytes())
print('Gear bounds:', box, 'dock artwork:', image.size)
