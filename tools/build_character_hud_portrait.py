#!/usr/bin/env python3
"""Make an undistorted bust portrait from the currently approved gameplay sprite."""
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1]
image = Image.open(root / 'preview/assets/main-character.png').convert('RGBA')
if image.size != (432, 800):
    raise ValueError('Unexpected character canvas; review portrait crop first')
# Square crop includes the whole hat, face and upper shoulders, without redrawing.
portrait = image.crop((0, 0, 432, 432)).resize((512, 512), Image.Resampling.LANCZOS)
path = root / 'preview/assets/character-hud-current.png'
portrait.save(path, optimize=True)
(root / 'app/src/main/res/drawable-nodpi/character_hud_current.png').write_bytes(path.read_bytes())
