#!/usr/bin/env python3
"""Generate continuous whole-image deformation frames; no cut limb regions or patches."""
import math
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
N=12

def smooth(a,b,v):
    t=max(0,min(1,(v-a)/(b-a)));return t*t*(3-2*t)

def deform(x,y,w,h,phase,running=False,back=False):
    u=x/w;v=y/h
    swing=math.sin(phase)
    # Smoothly blend displacement in space: a shoulder stays nearly fixed,
    # the forearm and hand travel as a connected region, legs alternate.
    left=1-smooth(.27,.43,u);right=smooth(.57,.73,u)
    arm=smooth(.43,.72,v)*(1-smooth(.76,.81,v))
    leg=smooth(.72,.92,v)
    dx=w*swing*((.080 if running else .070)*arm*(left-right) + (.070 if running else .060)*leg*(smooth(.52,.66,u)-(1-smooth(.34,.48,u))))
    # Near the foot, raise one leg while leaving the opposite leg on the baseline.
    ly=smooth(.70,.84,v)*(.035 if running else .022)*h
    foot_left=1-smooth(.44,.51,u);foot_right=smooth(.49,.56,u)
    dy=ly*(max(0,swing)*foot_left+max(0,-swing)*foot_right)
    if running:
        # Lean into travel and add a short airborne phase twice per stride.
        # This is a continuous image warp, not a skeletal or rotoscoped gait.
        lean = -.035*w*(1-smooth(.42,.98,v))
        flight = .018*h*max(0, math.cos(2*phase))
        # Draw trailing feet upward more strongly while the opposite leg advances.
        dy += .025*h*leg*(max(0,-swing)*foot_left+max(0,swing)*foot_right)
        dx += lean
        dy += flight
        # Subtle localized bounce where the pack is drawn; smoothly blended
        # so it stays connected to the sprite rather than leaving a cutout.
        pack_x = (smooth(.20,.30,u)*(1-smooth(.62,.73,u)) if back
                  else smooth(.64,.76,u))
        pack_y = smooth(.38,.46,v)*(1-smooth(.66,.73,v))
        dy += .012*h*math.sin(2*phase-.8)*pack_x*pack_y
    return (x-dx,y+dy)

for mode in ('walk','run'):
    for facing,name in [('front','main-character.png'),('back','main-character-back.png')]:
        src=Image.open(ROOT/'preview/assets'/name).convert('RGBA').resize((216,400),Image.Resampling.LANCZOS)
        w,h=src.size
        for i in range(N):
            phase=2*math.pi*i/N;mesh=[]
            for y in range(0,h,10):
                for x in range(0,w,9):
                    x2=min(w,x+9);y2=min(h,y+10)
                    a=deform(x,y,w,h,phase,mode=='run',facing=='back');b=deform(x,y2,w,h,phase,mode=='run',facing=='back');c=deform(x2,y2,w,h,phase,mode=='run',facing=='back');d=deform(x2,y,w,h,phase,mode=='run',facing=='back')
                    mesh.append(((x,y,x2,y2),a+b+c+d))
            frame=src.transform((w,h),Image.Transform.MESH,mesh,resample=Image.Resampling.BILINEAR)
            dest=ROOT/f'preview/assets/character-{mode}/{facing}-{i:02}.png';dest.parent.mkdir(parents=True,exist_ok=True);frame.save(dest,optimize=True)
            (ROOT/f'app/src/main/res/drawable-nodpi/character_{mode}_{facing}_{i:02}.png').write_bytes(dest.read_bytes())
