from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
portable=s[s.index('private void drawPortableTerrain'):s.index('private void drawIslandShadow')]
assert '0xff75cbd0' in portable
assert 'canvas.saveLayerAlpha' in portable and '163,Canvas.ALL_SAVE_FLAG' in portable
assert 'blend(palette.soilLight,0xffdac695,0.45)' in portable
assert 'worldTerrain.tile(column,row+1)==3' in portable and 'worldTerrain.tile(column+1,row)==3' in portable
assert 'if (!leftWater)' in portable and 'if (!rightWater)' in portable and 'palette.soilLight,true' in portable and 'palette.soilLight,false' in portable
assert 'paint.setColor(0x80d8faf1)' in portable and 'canvas.restore()' in portable
assert 'visualWaterDepth()' in s and 'canvas.clipRect' in s and 'paint.setAlpha(89)' in s
print('Android flat water tint, translucent layer and ripple wiring: static checks OK')
