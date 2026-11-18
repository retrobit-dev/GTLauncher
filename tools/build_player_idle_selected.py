#!/usr/bin/env python3
"""Extract selected idle pose concepts and their in-between studies without changing standing art."""
from collections import deque
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]

def extract(path):
    im=Image.open(path).convert('RGBA');w,h=im.size;p=im.load();seen=bytearray(w*h);queue=deque()
    def visit(x,y):
        i=y*w+x
        if seen[i]:return
        seen[i]=1;r,g,b,_=p[x,y]
        if r>=190 and g>=180 and b>=155 and abs(r-g)<40 and abs(g-b)<45:queue.append((x,y))
    for x in range(w):visit(x,0);visit(x,h-1)
    for y in range(h):visit(0,y);visit(w-1,y)
    while queue:
        x,y=queue.popleft();p[x,y]=(0,0,0,0)
        if x:visit(x-1,y)
        if x+1<w:visit(x+1,y)
        if y:visit(x,y-1)
        if y+1<h:visit(x,y+1)
    box=im.getbbox()
    if not box or box[0]==0 or box[1]==0 or box[2]==w or box[3]==h:raise ValueError(f'Extraction touches edge: {path}: {box}')
    return im.crop(box),box

def main():
    for view in ('front','back'):
        standing=Image.open(ROOT/('preview/assets/main-character.png' if view=='front' else 'preview/assets/main-character-back.png')).convert('RGBA')
        for pose in ('transition','concept'):
            suffix='-transition' if pose=='transition' else ''
            src=ROOT/f'preview/assets/character-redraw/idle-bow-redesign-{view}{suffix}.png'
            im,box=extract(src)
            # Body-size ceiling is shared; never reduce the entire sprite solely to simulate a bend.
            factor=min(500/im.width,(760 if pose=='transition' else 720)/im.height)
            im=im.resize((round(im.width*factor),round(im.height*factor)),Image.Resampling.LANCZOS)
            feet=im.getchannel('A').crop((0,im.height-40,im.width,im.height)).getbbox()
            target=standing.getchannel('A').crop((0,760,432,800)).getbbox()
            x=max(0,min(512-im.width,round(40+(target[0]+target[2]-feet[0]-feet[2])/2)))
            canvas=Image.new('RGBA',(512,800));canvas.alpha_composite(im,(x,800-im.height))
            padded=Image.new('RGBA',(512,800));padded.alpha_composite(standing,(40,0))
            # Ground contact and boots remain exactly the standing sprite in every frame.
            join=Image.new('L',(512,800));join.putdata([round(255*min(1,max(0,(y-682)/30))) for y in range(800) for _ in range(512)])
            canvas=Image.composite(padded,canvas,join)
            name=f'player-idle-{view}-{pose}.png';dst=ROOT/'preview/assets'/name;canvas.save(dst,optimize=True)
            (ROOT/f'app/src/main/res/drawable-nodpi/{name.replace("-","_")}').write_bytes(dst.read_bytes())
            print(view,pose,'crop',box,'result',canvas.getbbox())
if __name__=='__main__':main()
