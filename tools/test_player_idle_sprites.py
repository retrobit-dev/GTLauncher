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
        assert im.size==(432,800) and im.getchannel('A').getbbox()[3]==800,name
        assert im.getpixel((0,0))[3]==0 and im.getpixel((431,799))[3]==0,name
        standing=Image.open(root/('preview/assets/main-character-back.png' if side=='back' else 'preview/assets/main-character.png')).convert('RGBA')
        feet=im.getchannel('A').crop((0,760,432,800)).getbbox()
        reference=standing.getchannel('A').crop((0,760,432,800)).getbbox()
        assert abs((feet[0]+feet[2]-reference[0]-reference[2])/2)<=3, (name,feet,reference)
        assert abs((feet[2]-feet[0])-(reference[2]-reference[0]))<=15, (name,feet,reference)
        assert im.crop((0,712,432,800)).tobytes()==standing.crop((0,712,432,800)).tobytes(),name
        assert im.getchannel('A').getextrema()==(0,255),name
print('Exhausted pose assets: transparent, consistent anchor, browser/Android parity: OK')
