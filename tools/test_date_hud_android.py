from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
ns='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(root/'app/src/main/res/layout/activity_home.xml').getroot()
hud=layout.find(".//*[@"+ns+"id='@+id/home_date_hud']")
assert hud is not None and hud.get(ns+'layout_gravity') == 'top|right'
assert hud.get(ns+'clickable') == hud.get(ns+'focusable') == 'false'
clocks=hud.findall('.//TextClock');assert len(clocks)==4
for c in clocks:
 assert c.get(ns+'timeZone') is None and c.get(ns+'fontFamily')=='@font/press_start_2p'
assert [c.get(ns+'format24Hour') for c in clocks]==['HH:mm','MM/yy','d','EEE']
assert [c.get(ns+'format12Hour') for c in clocks]==['HH:mm','MM/yy','d','EEE']
calendar=hud.find(".//*[@"+ns+"background='@drawable/bg_date_hud_calendar']")
assert [c.get(ns+'id') for c in calendar.findall('TextClock')]==['@+id/hud_month','@+id/hud_day','@+id/hud_weekday']
for f in ['bg_date_hud_panel.xml','bg_date_hud_calendar.xml']: ET.parse(root/'app/src/main/res/drawable'/f)
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
# The drawer is an overlay; opening it must not hide the underlying world HUD.
drawer=activity.split('private void openDrawer()',1)[1].split('private void closeDrawer()',1)[0]
assert 'drawerScreen.setVisibility(View.VISIBLE)' in drawer
assert 'homeDateHud.setVisibility(View.GONE)' not in drawer
print('Android date HUD XML and drawer integration OK (static only)')

assert calendar.get(ns+'layout_width') == calendar.get(ns+'layout_height') == '48dp'
time=hud.find(".//*[@"+ns+"id='@+id/hud_time']")
panel=next(el for el in hud.iter() if time in list(el))
assert panel.get(ns+'layout_width')=='83dp' and panel.get(ns+'layout_height')=='0dp' and panel.get(ns+'layout_weight')=='1'
assert hud.find(".//*[@"+ns+"id='@+id/hud_season_icon']") is not None

assert clocks[2].get(ns+'layout_marginTop') == clocks[3].get(ns+'layout_marginTop') == '2dp'

assert 'updateSeason(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);' in activity
assert 'updateSeason(currentLatitude, currentLongitude, false);' in activity
assert '(!fallback && !hasPreciseLocationPermission())' in activity
assert 'seasonFallback != fallback' in activity

assert hud.get(ns+'scaleX') == hud.get(ns+'scaleY') == '0.75'
assert hud.get(ns+'transformPivotY') == '0dp'
assert 'view.setPivotX(right - left);' in activity and 'view.setPivotY(0f);' in activity
