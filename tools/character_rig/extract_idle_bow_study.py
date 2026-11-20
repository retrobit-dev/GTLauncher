#!/usr/bin/env python3
"""Hand-marked source-pixel studies for the exhausted pose, NOT runtime layers.

The cuts preserve the original 432x800 coordinate space and never invent occluded pixels.
Their boundaries intentionally have not been painted over or approved as animation joints.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'preview/assets/character-redraw/idle-bow-manual-study'
SPECS={
 'front':{
  'head':[(0,0),(431,0),(431,326),(362,338),(283,343),(96,345),(39,325),(0,305)],
  'torso':[(81,339),(361,335),(415,405),(406,512),(353,554),(83,554),(34,490)],
  'near_hand':[(310,482),(374,482),(390,516),(390,589),(365,606),(309,605),(298,570),(299,514)],
  'lower_body':[(52,529),(358,529),(399,616),(399,799),(38,799),(38,605)],
 },
 'back':{
  'head':[(0,0),(431,0),(431,302),(354,344),(280,362),(100,360),(23,321),(0,292)],
  'torso':[(58,348),(325,345),(385,439),(399,550),(353,578),(48,578),(40,423)],
  'near_hand':[(312,480),(373,482),(400,540),(400,589),(375,606),(326,601),(300,558)],
  'lower_body':[(58,544),(347,544),(393,624),(393,799),(51,799),(51,610)],
 },
}

def main():
 OUT.mkdir(parents=True,exist_ok=True)
 for side,parts in SPECS.items():
  path=ROOT/('preview/assets/main-character.png' if side=='front' else 'preview/assets/main-character-back.png')
  source=Image.open(path).convert('RGBA')
  if source.size!=(432,800):raise ValueError(f'Unexpected dimensions: {path}')
  for name,polygon in parts.items():
   mask=Image.new('L',source.size);ImageDraw.Draw(mask).polygon(polygon,fill=255)
   layer=source.copy();layer.putalpha(ImageChops.multiply(mask,source.getchannel('A')))
   if not layer.getbbox():raise ValueError(f'Empty: {side}/{name}')
   layer.save(OUT/f'{side}-{name}.png',optimize=True)

if __name__=='__main__':main()
