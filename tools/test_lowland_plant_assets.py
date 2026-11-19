from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
for name in ('cemara-laut','bush-jasmine','bush-mulberry'):
    a=root/f'preview/assets/{name}.png';b=root/f'app/src/main/res/drawable-nodpi/{name.replace("-","_")}.png'
    assert a.read_bytes()==b.read_bytes(),name
    im=Image.open(a).convert('RGBA');assert im.getpixel((0,0))[3]==0 and im.getbbox()[3]==im.height,name
for old in ('conifer-tree.png','bush-flower.png','bush-berry.png'):
    assert (root/'preview/assets'/old).exists(),old
print('Lowland plant sprite transparency, platform parity and preserved artwork: OK')
