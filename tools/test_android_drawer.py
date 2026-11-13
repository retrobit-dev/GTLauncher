from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
layout=ET.parse(root/'app/src/main/res/layout/activity_home.xml').getroot()
a='{http://schemas.android.com/apk/res/android}'
def id_of(node): return node.attrib.get(a+'id','')
phone=next(n for n in layout.iter() if id_of(n)=='@+id/home_phone_button')
overlay=next(n for n in layout.iter() if id_of(n)=='@+id/app_drawer_screen')
window=next(n for n in overlay.iter() if id_of(n)=='@+id/app_drawer_window')
header=next(n for n in window if n.tag=='LinearLayout')
assert phone.tag=='ImageButton' and phone.attrib[a+'src']=='@drawable/dock_phone'
assert overlay.tag=='FrameLayout' and window.attrib[a+'layout_gravity']=='center'
assert id_of(list(header)[-1])=='@+id/drawer_back' and list(header)[-1].attrib[a+'text']=='×'
java=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'home_phone_button).setOnClickListener(view -> openDrawer())' in java
part=java.split('private void openDrawer()',1)[1].split('private void closeDrawer()',1)[0]
assert 'drawerScreen.setVisibility(View.VISIBLE)' in part and 'homeScreen.setVisibility(View.GONE)' not in part
assert 'queryIntentActivities(query, 0)' in java and 'setComponent(new ComponentName(app.packageName, app.activityName))' in java
ET.parse(root/'app/src/main/res/drawable/bg_app_drawer_window.xml')
print('Android phone button, centered overlay, close position, package query and launch: static checks OK')
