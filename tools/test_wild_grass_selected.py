from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
a=root/'preview/assets/wild-grass-selected.png'
b=root/'app/src/main/res/drawable-nodpi/wild_grass_selected.png'
assert a.read_bytes()==b.read_bytes()
im=Image.open(a).convert('RGBA')
assert im.size==(512,256)
assert im.getpixel((0,0))[3]==0 and im.getpixel((511,255))[3]==0
box=im.getbbox();assert box and box[0]>30 and box[2]<482 and box[3]==256
# Representative foliage is sage rather than the saturated yellow-green concept.
colors=[(r,g,b) for r,g,b,a in im.getdata() if a>240 and g>r and g>b and g>80]
assert colors and sum(g-b for r,g,b in colors)/len(colors)<50
assert sum(a>128 for r,g,b,a in (im.getpixel((x,255)) for x in range(512)))>0
assert (root/'app/src/main/res/drawable-nodpi/wild_grass.png').exists(), 'keep original artwork'
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
assert 'R.drawable.wild_grass_selected' in view
print('Selected wild-grass sprite: transparency, grounding, asset parity and old artwork: OK')

preview=(root/'preview/app.js').read_text()
assert "if(tree.kind==='wild-grass'||tree.kind==='bush') centre.y+=0.75" in preview
assert "ctx.ellipse(centre.x,bottom,5,0.65" in preview
assert 'o.material.equals("preview-bush")?.75f' in view and 'contactWidth=grass?5f' in view
assert "if(tree.kind==='wild-grass'||tree.kind==='bush')continue" in preview
