from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
for side in ('front','back'):
    standing=Image.open(root/('preview/assets/main-character-back.png' if side=='back' else 'preview/assets/main-character.png')).convert('RGBA')
    padded=Image.new('RGBA',(512,800));padded.alpha_composite(standing,(40,0))
    for pose in ('transition','concept'):
        name=f'player-idle-{side}-{pose}.png'
        preview=root/'preview/assets'/name
        android=root/'app/src/main/res/drawable-nodpi'/name.replace('-','_')
        assert preview.read_bytes()==android.read_bytes(),name
        im=Image.open(preview).convert('RGBA')
        assert im.size==(512,800) and im.getchannel('A').getbbox()[3]==800,name
        assert im.getpixel((0,0))[3]==0 and im.getpixel((511,799))[3]==0,name
        assert im.crop((0,712,512,800)).tobytes()==padded.crop((0,712,512,800)).tobytes(),name
        assert im.getchannel('A').getextrema()==(0,255),name
        assert 20<=im.getchannel('A').getbbox()[1]<=100, 'pose should keep player size: '+name
print('Selected idle sprites: size, transparency, fixed boots, browser/Android parity: OK')
