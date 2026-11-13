from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
res=root/'app/src/main/res'
a='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(res/'layout/activity_home.xml').getroot()
hud=layout.find(".//*[@"+a+"id='@+id/home_character_hud']")
assert hud is not None and hud.get(a+'layout_gravity')=='top|left'
assert hud.get(a+'visibility') not in ('gone','invisible')
assert hud.get(a+'clickable')=='false' and hud.get(a+'focusable')=='false'
assert len(hud.findall('.//TextView'))==4
for t in hud.findall('.//TextView'): assert t.get(a+'text')=='100' and t.get(a+'fontFamily')=='@font/press_start_2p'
for name in ['health','stamina','food','water']:
 assert len([v for v in hud.findall('.//View') if v.get(a+'background')=='@drawable/hud_stat_'+name])==10
 ET.parse(res/'drawable'/('hud_stat_icon_'+name+'.xml'))
assert (res/'drawable-nodpi/character_hud_portrait.png').exists()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'home_character_hud' not in activity # No world switch hides the launcher HUD.
print('Android character HUD is present independent of selected world: XML static checks OK')
