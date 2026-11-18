from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
for side in ('front','back'):
    for pose in ('transition','concept'):
        name=f'player-idle-{side}-{pose}.png'
        preview=root/'preview/assets'/name
        android=root/'app/src/main/res/drawable-nodpi'/name.replace('-','_')
        assert preview.read_bytes()==android.read_bytes(),name
        im=Image.open(preview).convert('RGBA')
        assert im.size==(432,800) and im.getchannel('A').getbbox()[3]==798,name
        assert im.getpixel((0,0))[3]==0 and im.getpixel((431,799))[3]==0,name
        assert im.getchannel('A').getextrema()==(0,255),name
print('Exhausted pose assets: transparent, consistent anchor, browser/Android parity: OK')
