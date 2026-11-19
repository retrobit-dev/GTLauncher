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
assert (root/'app/src/main/res/drawable-nodpi/wild_grass.png').exists(), 'keep original artwork'
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
assert 'R.drawable.wild_grass_selected' in view
print('Selected wild-grass sprite: transparency, grounding, asset parity and old artwork: OK')
