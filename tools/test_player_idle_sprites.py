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
for side in ('front','back'):
    for pose in ('sitting','sit-lowering','sit-nearly'):
        name=f'player-idle-{side}-{pose}.png'
        preview=root/'preview/assets'/name
        android=root/'app/src/main/res/drawable-nodpi'/name.replace('-','_')
        assert preview.read_bytes()==android.read_bytes()
        im=Image.open(preview).convert('RGBA')
        assert im.size==(1280,800) and im.getpixel((0,0))[3]==0 and im.getchannel('A').getbbox()[3]>=790
for side,pos in [('front',(447,68)),('back',(386,94))]:
    original=Image.open(root/('preview/assets/main-character.png' if side=='front' else 'preview/assets/main-character-back.png')).convert('RGBA')
    for pose in ('sitting','sit-nearly'):
        im=Image.open(root/f'preview/assets/player-idle-{side}-{pose}.png').convert('RGBA')
        assert im.crop((pos[0],pos[1],pos[0]+432,pos[1]+190)).tobytes()==original.crop((0,0,432,190)).tobytes(), 'original hat must be kept'
# The new seated frames are placed at their source artwork pixel scale, not fit
# independently into a target box. Samples are inside the overalls/backpack.
for side,frame,source_name,pt,shift in [
    ('front','sitting','sit-front-hands-corrected.png',(630,520),(-13,6)),
    ('front','sit-nearly','sit-redraw-near-front.png',(630,520),(-13,6)),
    ('back','sitting','sit-redraw-final-back.png',(600,600),(-13,39)),
    ('back','sit-nearly','sit-redraw-near-back.png',(600,600),(-13,39)),
]:
    source=Image.open(root/'preview/assets/character-redraw'/source_name).convert('RGBA')
    rendered=Image.open(root/f'preview/assets/player-idle-{side}-{frame}.png').convert('RGBA')
    assert rendered.getpixel((pt[0]+shift[0],pt[1]+shift[1]))==source.getpixel(pt), (side,frame,'body was rescaled')
front=Image.open(root/'preview/assets/player-idle-front-sit-nearly.png').convert('RGBA')
def skin(pixel):
    r,g,b,a=pixel
    return a>64 and r>150 and r>g*1.25 and r>b*1.25 and g>65
for pose in ('sitting','sit-nearly'):
    im=Image.open(root/f'preview/assets/player-idle-front-{pose}.png').convert('RGBA')
    assert sum(map(skin,im.crop((925,560,960,715)).getdata()))==0, 'duplicate far forearm must be occluded'
    assert sum(map(skin,im.crop((720,675,790,738)).getdata()))>500, 'far hand should emerge beside the hip'
    assert sum(map(skin,im.crop((480,535,530,630)).getdata()))>200, 'a small section of the far arm should remain visible'
    assert not any(a and r>210 and g>195 and b>175 and abs(r-g)<35 and abs(g-b)<40 for r,g,b,a in im.crop((730,665,800,735)).getdata()), 'no trapped cream background around the hand'
# The near supporting hand must move backward on the image, without shifting
# the whole body or losing the separately occluded far hand.
def hand_mean_x(image, dx, dy):
    xs=[x for y in range(710+dy,750+dy) for x in range(760+dx,930+dx) if skin(image.getpixel((x,y)))]
    assert xs
    return sum(xs)/len(xs)-dx
old=Image.open(root/'preview/assets/character-redraw/sit-front-hands-corrected.png').convert('RGBA')
for pose in ('sitting','sit-nearly'):
    image=Image.open(root/f'preview/assets/player-idle-front-{pose}.png').convert('RGBA')
    assert hand_mean_x(image,-13,6)>hand_mean_x(old,0,0)+20, 'supporting hand should be farther back'
print('Selected idle sprites: size, transparency, fixed boots, browser/Android parity: OK')
