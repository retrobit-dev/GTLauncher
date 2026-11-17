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
assert not hud.findall('.//TextView')
portrait_view,stats=list(hud)
assert portrait_view.get(a+'background') == '@drawable/status_frame'
assert hud.get(a+'layout_height') == portrait_view.get(a+'layout_height') == '48dp'
assert stats.get(a+'layout_height') == 'match_parent'
assert all(row.get(a+'background') is None for row in stats)
assert (res/'drawable-xhdpi/status_frame.9.png').exists()
assert [row.get(a+'contentDescription').split(':')[0] for row in stats] == ['Health','Stamina','Makanan','Air']
assert all(row.get(a+'layout_width') == 'match_parent' and row.get(a+'layout_weight') == '1' and '20 poin' in row.get(a+'contentDescription') for row in stats)
for row,name in zip(stats,['health','stamina','food','water']):
 icons=row.findall('ImageView')
 assert len(icons)==5 and all(icon.get(a+'src')=='@drawable/hud_stat_icon_'+name for icon in icons)
 ET.parse(res/'drawable'/('hud_stat_icon_'+name+'.xml'))
assert ET.parse(res/'drawable/hud_stat_icon_food.xml').getroot().find('path').get(a+'fillColor').lower() == '#6d9134'
import struct
portrait=(res/'drawable-nodpi/character_hud_current.png').read_bytes()
assert portrait==(root/'preview/assets/character-hud-current.png').read_bytes()
assert portrait[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',portrait[16:24])==(512,512) and portrait[25]==6
assert portrait_view.get(a+'src')=='@drawable/character_hud_current'
assert 'character-hud-current.png' in (root/'preview/index.html').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'home_character_hud' not in activity # No world switch hides the launcher HUD.
print('Android character HUD is present independent of selected world: XML static checks OK')
