from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
im=Image.open(root/'preview/assets/conifer-tree.png').convert('RGBA')
assert im.size==(512,490) and im.getpixel((0,0))[3]==0
alpha=im.getchannel('A')
assert alpha.getbbox()[3]==490
xs=[x for x in range(512) if alpha.getpixel((x,480))>0]
assert xs and abs((min(xs)+max(xs))/2-256)<3
assert (root/'preview/assets/orange-tree-pixel.png').exists()
print('Conifer sprite transparency, size and centered root: OK')
