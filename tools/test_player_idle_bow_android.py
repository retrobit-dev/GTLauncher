from pathlib import Path
s=Path('app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
t=Path('app/src/main/java/dev/retrobit/gtlauncher/PlayerIdleBow.java').read_text()
assert 'DELAY = 20000, DURATION = 4000' in t
assert 'updateIdleBow(frameTime);' in s
assert 'characterStepping && animationEnabled' in s
assert '!characterStepping && !portalWalkActive' in s
assert 'android.animation.ValueAnimator.areAnimatorsEnabled()' in s
assert 'if(idleBowAmount>0 && !characterStepping && idleBowBitmap!=null)return idleBowBitmap;' in s
assert 'onWindowVisibilityChanged' in s
print('Android idle timing, movement, shadow sprite and reduced-motion wiring: static checks OK')
