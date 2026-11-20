from pathlib import Path
import struct
root=Path(__file__).resolve().parents[1]
icon=(root/'preview/assets/hud/dirt-block.png').read_bytes()
assert icon[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',icon[16:24])==(256,256) and icon[25]==6
preview=(root/'preview/app.js').read_text()
android=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
assert "baseMaterial === 2 ? blendColor(palette.soilLight, '#d49d65', 0.4)" in preview
assert 'baseMaterial==2 ? blend(palette.soilLight,0xffd49d65,0.4)' in android
assert 'if (material === 6)' in preview and 'if(material==6)' in android
assert "6:'#bd7549'" in preview and '0xffbd7549' in android
assert '[-.60,-.20,.20,.60]' in preview and '-.60f,-.20f,.20f,.60f' in android
print('Dirt block thumbnail, matched soil palette and distinct farmland: static OK')
