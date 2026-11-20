#!/usr/bin/env python3
"""Extract measurable source-pixel references for a future seated redraw.

These masks are studies, not usable joints: hidden cloth and the surfaces exposed
by sitting must be drawn. Every visible pixel retains its standing-sprite position.
"""
from pathlib import Path
import json
from PIL import Image, ImageChops, ImageDraw

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'preview/assets/character-redraw/sitting-reference-parts'
PARTS={
 'front': {
  'head':[(0,0),(431,0),(431,280),(390,280),(375,310),(350,332),(310,345),(250,355),(190,355),(130,346),(80,323),(40,290),(0,280)],
  'torso':[(100,350),(260,340),(320,350),(355,390),(330,445),(310,490),(305,545),(290,565),(105,565),(90,550),(90,510),(85,450),(60,415)],
  'far_leg':[(85,590),(190,590),(205,620),(205,680),(202,799),(45,799),(45,700),(85,675)],
  'near_leg':[(200,590),(300,590),(330,600),(335,680),(335,799),(202,799),(202,690)],
 },
 'back': {
  'head':[(0,0),(431,0),(431,265),(390,265),(375,320),(335,350),(250,360),(140,360),(45,330),(0,290)],
  'torso':[(70,350),(270,340),(327,370),(350,420),(310,465),(290,530),(290,565),(85,565),(60,545),(45,430)],
  'far_leg':[(100,590),(190,590),(205,620),(205,680),(205,799),(90,799),(90,700),(100,675)],
  'near_leg':[(200,590),(300,590),(330,600),(335,680),(375,799),(205,799),(205,690)],
 },
}
# Approximate artwork landmarks, not measurements of a physical skeleton. The
# upper thigh is partly covered; redrawing the hidden hip is still necessary.
LANDMARKS={
 'front':{'far_hip':[145,590],'far_ankle':[145,690],'near_hip':[260,590],'near_ankle':[260,690],'boot_bottom':800},
 'back':{'far_hip':[150,590],'far_ankle':[150,690],'near_hip':[260,590],'near_ankle':[260,690],'boot_bottom':800},
}
def main():
 OUT.mkdir(parents=True,exist_ok=True)
 for view,parts in PARTS.items():
  source=Image.open(ROOT/('preview/assets/main-character.png' if view=='front' else 'preview/assets/main-character-back.png')).convert('RGBA')
  if source.size!=(432,800):raise ValueError(f'unexpected source size: {view}')
  for name,polygon in parts.items():
   mask=Image.new('L',source.size);ImageDraw.Draw(mask).polygon(polygon,fill=255)
   part=source.copy();part.putalpha(ImageChops.multiply(source.getchannel('A'),mask))
   if not part.getbbox():raise ValueError(f'empty mask: {view}/{name}')
   part.save(OUT/f'{view}-{name}.png',optimize=True)
 (OUT/'landmarks.json').write_text(json.dumps({'source_size':[432,800], 'coordinate_system':'standing sprite pixels', 'landmarks':LANDMARKS},indent=2)+'\n')
if __name__=='__main__':main()
