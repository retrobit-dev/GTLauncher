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
texts=hud.findall('.//TextView')
assert [t.get(a+'text') for t in texts] == ['HEALTH','100','STAMINA','100','FOOD','100','DRINK','100']
for t in texts: assert t.get(a+'fontFamily')=='@font/press_start_2p' and t.get(a+'textColor')=='#342B1E'
top,needs=list(hud)
portrait_view,vitals=list(top)
assert portrait_view.get(a+'background') == '@drawable/status_frame'
assert all(row.get(a+'background') == '@drawable/status_frame' for row in list(vitals)+list(needs))
assert (res/'drawable-xhdpi/status_frame.9.png').exists()
assert [row.get(a+'contentDescription').split(':')[0] for row in vitals] == ['Health','Stamina']
assert [row.get(a+'contentDescription').split(':')[0] for row in needs] == ['Makanan','Air']
for name in ['health','stamina','food','water']:
 assert len([v for v in hud.findall('.//View') if v.get(a+'background')=='@drawable/hud_stat_'+name])==10
 ET.parse(res/'drawable'/('hud_stat_icon_'+name+'.xml'))
import struct
portrait=(res/'drawable-nodpi/character_hud_current.png').read_bytes()
assert portrait==(root/'preview/assets/character-hud-current.png').read_bytes()
assert portrait[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',portrait[16:24])==(512,512) and portrait[25]==6
assert portrait_view.get(a+'src')=='@drawable/character_hud_current'
assert 'character-hud-current.png' in (root/'preview/index.html').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'home_character_hud' not in activity # No world switch hides the launcher HUD.
print('Android character HUD is present independent of selected world: XML static checks OK')
