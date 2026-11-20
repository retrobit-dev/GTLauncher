from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
res=root/'app/src/main/res'
a='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(res/'layout/activity_home.xml').getroot()
hud=layout.find(".//*[@"+a+"id='@+id/home_character_hud']")
assert hud is not None and hud.get(a+'layout_gravity')=='top|left'
assert hud.get(a+'visibility') not in ('gone','invisible')
assert hud.get(a+'alpha') == '0.85'
assert all(node.get(a+'alpha') is None for node in hud.iter() if node is not hud)
assert hud.get(a+'clickable')=='false' and hud.get(a+'focusable')=='false'
assert not hud.findall('.//TextView')
portrait_view,stats=list(hud)
assert hud.get(a+'background') == '@drawable/character_status_frame'
assert (res/'drawable-xhdpi/character_status_frame.9.png').exists()
assert portrait_view.get(a+'background') is None
assert hud.get(a+'layout_height') == portrait_view.get(a+'layout_height') == '48dp'
assert stats.get(a+'layout_height') == 'match_parent'
assert stats.get(a+'background') == '@drawable/bg_hud_status_rows'
assert [row.get(a+'background') for row in stats] == [None, '@drawable/bg_hud_row_separator', '@drawable/bg_hud_row_separator']
assert [row.get(a+'contentDescription').split(':')[0] for row in stats] == ['Health','Stamina','Makanan']
assert all(row.get(a+'layout_width') == 'match_parent' and row.get(a+'layout_weight') == '1' and '100 dari 100' in row.get(a+'contentDescription') for row in stats)
for row,name in zip(stats,['health','stamina','food']):
 icon=row.find('ImageView'); bar=row.find('View')
 assert icon.get(a+'src')=='@drawable/status_icon_'+name
 assert bar.get(a+'background')=='@drawable/status_bar_'+name
 assert (res/'drawable-nodpi'/('status_icon_'+name+'.png')).read_bytes() == (root/'preview/assets/hud'/('status-'+name+'.png')).read_bytes()
 layers=ET.parse(res/'drawable'/('status_bar_'+name+'.xml')).getroot()
 assert list(layers)[2].find('shape/gradient').get(a+'centerColor') == {'health':'#C94E43','stamina':'#E1A52E','food':'#6D9134'}[name]
import struct
portrait=(res/'drawable-nodpi/character_hud_current.png').read_bytes()
assert portrait==(root/'preview/assets/character-hud-current.png').read_bytes()
assert portrait[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',portrait[16:24])==(512,512) and portrait[25]==6
assert portrait_view.get(a+'src')=='@drawable/character_hud_current'
assert 'character-hud-current.png' in (root/'preview/index.html').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'home_character_hud' not in activity # No world switch hides the launcher HUD.
print('Android character HUD is present independent of selected world: XML static checks OK')
