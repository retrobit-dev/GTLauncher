from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
preview=root/'preview/assets/pet-sparrow.png'
assert preview.read_bytes()==(root/'app/src/main/res/drawable-nodpi/pet_sparrow.png').read_bytes()
im=Image.open(preview).convert('RGBA')
assert max(im.size)==192 and im.getpixel((0,0))[3]==0 and im.getchannel('A').getextrema()==(0,255)
flap=root/'preview/assets/pet-sparrow-flap.png'
assert flap.read_bytes()==(root/'app/src/main/res/drawable-nodpi/pet_sparrow_flap.png').read_bytes()
f=Image.open(flap).convert('RGBA')
assert f.size==im.size and f.crop((0,0,65,192)).tobytes()==im.crop((0,0,65,192)).tobytes() and f.tobytes()!=im.tobytes()
# Guard against layering the raised wing over the original folded wing.
from build_player_idle_selected import extract
clean,_=extract(root/'preview/assets/character-redraw/sparrow-flight-clean-study.png')
clean=clean.resize((192,192),Image.Resampling.LANCZOS)
assert f.crop((100,95,145,130)).tobytes()==clean.crop((100,95,145,130)).tobytes()
for name in ('pet-sparrow-back.png','pet-sparrow-back-flap.png'):
    file=root/'preview/assets'/name
    assert file.read_bytes()==(root/'app/src/main/res/drawable-nodpi'/name.replace('-','_')).read_bytes()
    frame=Image.open(file).convert('RGBA');assert frame.size==(192,192) and frame.getpixel((0,0))[3]==0
s=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
for text in ['R.drawable.pet_sparrow','R.drawable.pet_sparrow_flap','R.drawable.pet_sparrow_back','R.drawable.pet_sparrow_back_flap','drawSparrowPet(canvas,motionTime','seatWeight,mirror,back,depth,','!android.animation.ValueAnimator.areAnimatorsEnabled()']:
    assert text in s
print('Sparrow sprite parity and Android wiring: OK (static)')
