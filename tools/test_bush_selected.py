from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
a=root/'preview/assets/bush-selected.png';b=root/'app/src/main/res/drawable-nodpi/bush_selected.png'
assert a.read_bytes()==b.read_bytes()
im=Image.open(a).convert('RGBA');assert im.size==(512,512)
box=im.getbbox();assert box and box[0]>0 and box[2]<512 and box[3]==512
assert im.getpixel((0,0))[3]==0
assert (root/'app/src/main/res/drawable-nodpi/bush_round.png').exists()
print('Bush sprite transparency, grounded base, Android parity and legacy art: OK')

for variant in ('flower','berry'):
    a=root/f'preview/assets/bush-{variant}.png';b=root/f'app/src/main/res/drawable-nodpi/bush_{variant}_selected.png'
    assert a.read_bytes()==b.read_bytes()
    im=Image.open(a).convert('RGBA');assert im.size==(512,512) and im.getbbox()[3]==512 and im.getpixel((0,0))[3]==0
print('Flower and berry sprites: transparency, ground and parity OK')
