#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[1]
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
motion=(root/'app/src/main/java/dev/retrobit/gtlauncher/OrangeTreeMotion.java').read_text()
assert 'OrangeTreeMotion.sway(animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled()' in view
assert 'canvas.skew((float)(-sway/g.height()),0)' in view
assert 'hitX+=(y-bounds.bottom)*(float)(orangeTreeSway(o)/bounds.height())' in view
assert 'speed<=0' in motion and 'Math.min(2.8,speed*.26)' in motion
print('Android orange tree motion, reduced motion and hit-test wiring: static checks OK')
