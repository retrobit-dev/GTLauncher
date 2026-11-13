from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
ns='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(root/'app/src/main/res/layout/activity_home.xml').getroot()
hud=layout.find(".//*[@"+ns+"id='@+id/home_date_hud']")
def parent(el): return next(x for x in hud.iter() if el in list(x))
time=hud.find(".//*[@"+ns+"id='@+id/hud_time']")
battery=hud.find(".//*[@"+ns+"id='@+id/hud_battery']")
a,b=parent(time),parent(battery)
stack=parent(a); assert stack is parent(b)
assert list(stack)==[a,stack.find('View'),b]
assert stack.get(ns+'layout_height')=='41dp' and stack.get(ns+'layout_marginTop')=='7dp'
calendar=hud.find(".//*[@"+ns+"background='@drawable/bg_date_hud_calendar']")
assert calendar.get(ns+'layout_height')=='48dp'
assert 7+41==48
assert stack.find('View').get(ns+'layout_height')=='1dp'
assert b.get(ns+'layout_marginTop') is None
assert a.get(ns+'layout_weight')==b.get(ns+'layout_weight')=='1'
for prop in ['layout_width','layout_height','gravity','paddingStart','paddingEnd']:
 assert a.get(ns+prop)==b.get(ns+prop), prop
assert time.get(ns+'layout_marginStart')==battery.get(ns+'layout_marginStart')=='3dp'
assert a.get(ns+'paddingStart')==a.get(ns+'paddingEnd')=='8dp'
assert a.get(ns+'layout_width')==b.get(ns+'layout_width')=='83dp'
assert stack.get(ns+'layoutDirection')=='ltr'
for value in [time,battery]:
 assert value.get(ns+'layout_width')=='0dp' and value.get(ns+'layout_weight')=='1'
 assert value.get(ns+'gravity')=='right|center_vertical'
assert len(b.findall('ImageView'))==1 and battery.get(ns+'text')=='--.--'
for icon in ['hud_battery_icon.xml','hud_battery_low.xml','hud_battery_charging.xml','hud_battery_unknown.xml']:
 ET.parse(root/'app/src/main/res/drawable'/icon)
s=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
for x in ['Intent.ACTION_BATTERY_CHANGED','BatteryManager.EXTRA_LEVEL','BatteryManager.EXTRA_SCALE', 'BatteryManager.EXTRA_STATUS', 'BatteryManager.BATTERY_STATUS_CHARGING', 'level * 5L <= scale', 'R.drawable.hud_battery_low', 'R.drawable.hud_battery_charging', 'R.drawable.hud_battery_unknown','"%.2f"','startBatteryUpdates();','stopBatteryUpdates();','unregisterReceiver(batteryReceiver)', 'resizeHudReadouts()', 'measureText("88:88")', 'value.length() > 5 ? 5f / 6f : 1f', 'dp(8) + dp(14) + dp(3) + dp(8)']:
 assert x in s,x
print('Android battery HUD layout and lifecycle: static checks OK')
