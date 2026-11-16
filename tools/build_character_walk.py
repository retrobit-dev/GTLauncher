#!/usr/bin/env python3
"""Generate continuous whole-image deformation frames; no cut limb regions or patches."""
import math
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
N=12

def smooth(a,b,v):
    t=max(0,min(1,(v-a)/(b-a)));return t*t*(3-2*t)

def deform(x,y,w,h,phase):
    u=x/w;v=y/h
    swing=math.sin(phase)
    # Smoothly blend displacement in space: a shoulder stays nearly fixed,
    # the forearm and hand travel as a connected region, legs alternate.
    left=1-smooth(.27,.43,u);right=smooth(.57,.73,u)
    arm=smooth(.43,.72,v)*(1-smooth(.76,.81,v))
    leg=smooth(.72,.92,v)
    dx=w*swing*(.030*arm*(left-right) + .035*leg*(smooth(.52,.66,u)-(1-smooth(.34,.48,u))))
    # Near the foot, raise one leg while leaving the opposite leg on the baseline.
    ly=smooth(.82,.96,v)*.008*h
    dy=ly*(max(0,swing)*left+max(0,-swing)*right)
    return (x-dx,y+dy)

for facing,name in [('front','main-character.png'),('back','main-character-back.png')]:
    src=Image.open(ROOT/'preview/assets'/name).convert('RGBA').resize((216,400),Image.Resampling.LANCZOS)
    w,h=src.size
    for i in range(N):
        phase=2*math.pi*i/N;mesh=[]
        for y in range(0,h,10):
            for x in range(0,w,9):
                x2=min(w,x+9);y2=min(h,y+10)
                a=deform(x,y,w,h,phase);b=deform(x,y2,w,h,phase);c=deform(x2,y2,w,h,phase);d=deform(x2,y,w,h,phase)
                mesh.append(((x,y,x2,y2),a+b+c+d))
        frame=src.transform((w,h),Image.Transform.MESH,mesh,resample=Image.Resampling.BILINEAR)
        dest=ROOT/f'preview/assets/character-walk/{facing}-{i:02}.png';frame.save(dest,optimize=True)
        (ROOT/f'app/src/main/res/drawable-nodpi/character_walk_{facing}_{i:02}.png').write_bytes(dest.read_bytes())
