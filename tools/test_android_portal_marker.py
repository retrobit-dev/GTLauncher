from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'drawPortalMarkers(scene);' in view
assert 'portalMarkerBounds(portal).contains(xy[0],xy[1])' in view
assert 'portalAt(event.getX(),event.getY())' in activity
assert 'setGravity(Gravity.CENTER)' in activity and 'FLAG_DIM_BEHIND' in activity
assert 'worldStore.travel(sourceId,portalId)' in activity
assert 'no.setOnClickListener(v->dialog.dismiss())' in activity
assert '@font/press_start_2p' in (root/'app/src/main/res/layout/activity_home.xml').read_text()
for name in ['bg_portal_dialog.xml','bg_portal_button.xml']:
 ET.parse(root/'app/src/main/res/drawable'/name)
assert (root/'app/src/main/res/drawable-nodpi/portal_exclamation.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
print('Android portal marker hit target and centered confirmation: static checks OK')
