from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
portable=s[s.index('private void drawPortableTerrain'):s.index('private void drawIslandShadow')]
assert '0xffb8b2a7' in portable and 'material==4 ? blend(colors[material],palette.soilDark,0.08)' in portable
assert 'if(material==4)' in portable and 'fissure.lineTo' in portable
print('Android stone palette and surface detail: static checks OK')
