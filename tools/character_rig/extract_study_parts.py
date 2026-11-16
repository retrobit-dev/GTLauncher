#!/usr/bin/env python3
"""Extract rough polygon-masked limb studies, not finished runtime rig artwork.

These source-space masks are hand placed on the generated running studies.
They intentionally retain visible pixels only: occluded regions, clean joints,
layer overlaps and pivots still need to be drawn and verified manually.
"""
from collections import deque
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'preview/assets/character-redraw/run-keyposes'
OUT = ROOT / 'preview/assets/character-redraw/run-rig/study-parts'
# 1024x1024 coordinates. Polygons isolate rough arms and legs, not complete anatomy.
MASKS = {
 'front': {
  'left_arm': [(393,442),(420,456),(412,522),(392,575),(345,593),(302,573),(267,527),(267,473),(298,432),(345,430)],
  'right_arm': [(620,438),(682,437),(728,461),(773,492),(797,541),(787,594),(749,623),(705,613),(679,586),(653,563),(626,548)],
  'left_leg': [(419,656),(508,653),(518,693),(482,752),(453,808),(429,870),(402,911),(349,913),(292,876),(256,833),(266,788),(314,766),(351,716)],
  'right_leg': [(506,657),(601,651),(638,681),(668,706),(699,679),(737,681),(773,708),(785,757),(781,797),(754,826),(709,819),(671,802),(627,806),(581,808),(545,791),(524,754),(500,721)],
 },
 'back': {
  'left_arm': [(334,447),(370,453),(371,505),(352,550),(329,606),(308,646),(278,650),(253,633),(256,598),(276,563),(286,517),(299,479)],
  'right_arm': [(566,437),(612,435),(641,457),(673,475),(691,458),(699,434),(718,419),(749,430),(770,453),(771,485),(748,516),(711,552),(677,578),(645,590),(620,578),(595,550)],
  'left_leg': [(417,648),(498,652),(523,682),(502,719),(476,757),(450,795),(413,811),(383,804),(362,825),(349,869),(323,883),(293,870),(264,832),(256,800),(263,754),(283,725),(315,714),(365,722),(396,699)],
  'right_leg': [(501,649),(594,645),(619,670),(636,711),(660,759),(686,794),(724,812),(760,827),(778,856),(772,885),(738,907),(687,916),(652,902),(620,872),(592,843),(567,805),(537,772),(511,735),(490,691)],
 }
}

def remove_background(image):
    image=image.convert('RGBA'); w,h=image.size; pixels=image.load(); bg=pixels[0,0][:3]
    seen=bytearray(w*h); q=deque()
    def visit(x,y):
        i=y*w+x
        if seen[i]: return
        seen[i]=1
        if max(abs(pixels[x,y][j]-bg[j]) for j in range(3)) <= 38: q.append((x,y))
    for x in range(w): visit(x,0); visit(x,h-1)
    for y in range(h): visit(0,y); visit(w-1,y)
    while q:
        x,y=q.popleft();pixels[x,y]=(0,0,0,0)
        if x: visit(x-1,y)
        if x+1<w: visit(x+1,y)
        if y: visit(x,y-1)
        if y+1<h: visit(x,y+1)
    return image

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for view, parts in MASKS.items():
        src=remove_background(Image.open(SOURCE / f'{view}-study.png'))
        if src.size!=(1024,1024): raise ValueError(f'unexpected source size: {view} {src.size}')
        for name,polygon in parts.items():
            mask=Image.new('L',src.size);ImageDraw.Draw(mask).polygon(polygon,fill=255)
            layer=src.copy();layer.putalpha(ImageChops.multiply(src.getchannel('A'),mask))
            if not layer.getbbox(): raise ValueError(f'empty mask: {view}/{name}')
            clean=Image.new('RGBA',src.size);clean.alpha_composite(layer)
            clean.save(OUT / f'{view}-{name}.png',optimize=True)

if __name__ == '__main__': main()
