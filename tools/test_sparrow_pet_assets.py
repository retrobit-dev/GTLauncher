from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
preview=root/'preview/assets/pet-sparrow.png'
assert preview.read_bytes()==(root/'app/src/main/res/drawable-nodpi/pet_sparrow.png').read_bytes()
im=Image.open(preview).convert('RGBA')
assert max(im.size)==192 and im.getpixel((0,0))[3]==0 and im.getchannel('A').getextrema()==(0,255)
s=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
for text in ['R.drawable.pet_sparrow','drawSparrowPet(canvas,motionTime','seatWeight,mirror,back,depth','!android.animation.ValueAnimator.areAnimatorsEnabled()']:
    assert text in s
print('Sparrow sprite parity and Android wiring: OK (static)')
