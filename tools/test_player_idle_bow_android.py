from pathlib import Path
s=Path('app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
t=Path('app/src/main/java/dev/retrobit/gtlauncher/PlayerIdleBow.java').read_text()
assert 'DELAY = 3000, DURATION = 2000, INTERVAL = 3000' in t
assert 'SIT_DELAY = 15000, SIT_TRANSITION = 1000' in t
assert 'visualWaterDepth()<=0' in s
assert 'idleFrontSitting' in s and 'idleBackSitting' in s
assert 'updateIdleBow(frameTime);' in s
assert 'characterStepping && animationEnabled' in s
assert '!characterStepping && !portalWalkActive' in s
assert 'android.animation.ValueAnimator.areAnimatorsEnabled()' in s
assert 'idleBowAmount*2' in s
assert 'PorterDuff.Mode.ADD' in s
assert 'sprite==idleBlendBitmap' in s
for name in ['idleFrontTransition','idleFrontPose','idleBackTransition','idleBackPose']:
    assert name in s
assert 'onWindowVisibilityChanged' in s
assert 'sprite.getWidth()/432f' in s
assert 'baseWidth/(width+Math.abs(ray[0]))' in s
print('Android idle timing, movement, shadow sprite and reduced-motion wiring: static checks OK')
