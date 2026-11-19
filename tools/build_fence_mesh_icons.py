#!/usr/bin/env python3
"""Render catalog icons from the same projected faces used by the preview renderer."""
from pathlib import Path
import json, subprocess
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
for material in ('wood','stone'):
    script="const m=require('./preview/fence-mesh.js'); console.log(JSON.stringify(m.faces({x:0,y:0},{x:14,y:7},process.argv[1])))"
    faces=json.loads(subprocess.check_output(['node','-e',script,material],cwd=ROOT))
    points=[p for face in faces for p in face['points']]
    xmin=min(p['x'] for p in points);xmax=max(p['x'] for p in points)
    ymin=min(p['y'] for p in points);ymax=max(p['y'] for p in points)
    scale=min(200/(xmax-xmin),200/(ymax-ymin));ox=128-(xmin+xmax)*scale/2;oy=128-(ymin+ymax)*scale/2
    canvas=Image.new('RGBA',(256,256));draw=ImageDraw.Draw(canvas)
    border='#687666' if material=='stone' else '#664936'
    for face in faces:
        polygon=[(round(ox+p['x']*scale),round(oy+p['y']*scale)) for p in face['points']]
        draw.polygon(polygon,fill=face['color'])
        if not face.get('detail'):draw.line(polygon+[polygon[0]],fill=border,width=max(1,round(.3*scale)),joint='curve')
    dst=ROOT/'preview/assets'/f'fence-mesh-{material}.png';canvas.save(dst,optimize=True)
    (ROOT/'app/src/main/res/drawable-nodpi'/f'fence_mesh_{material}.png').write_bytes(dst.read_bytes())
