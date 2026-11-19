#!/usr/bin/env python3
"""Create a centered transparent conifer sprite without distorting its proportions."""
from pathlib import Path
from PIL import Image
from build_player_idle_selected import extract
root=Path(__file__).resolve().parents[1]
im,_=extract(root/'preview/assets/character-redraw/conifer-pixel-concept.png')
im.thumbnail((500,490),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(512,490));canvas.alpha_composite(im,((512-im.width)//2,490-im.height))
canvas.save(root/'preview/assets/conifer-tree.png',optimize=True)
