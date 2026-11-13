from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'drawPortalMarkers(scene);' in view
assert 'portalMarkerVisible(portal) && portalMarkerBounds(portal).contains(x,y)' in view
assert '!portalWalkActive && showCharacter && characterColumn==portal.tile.column && characterRow==portal.tile.row && portal.edge.equals(characterFacing)' in view
assert 'portalMarkerAt(event.getX(),event.getY())' in activity
assert 'if(marker!=null) confirmPortalMarker(marker)' in activity
assert 'setGravity(Gravity.CENTER)' in activity and 'FLAG_DIM_BEHIND' in activity
assert 'worldStore.travel(sourceId,portalId)' in activity
assert 'no.setOnClickListener(v->dialog.dismiss())' in activity
assert '@font/press_start_2p' in (root/'app/src/main/res/layout/activity_home.xml').read_text()
for name in ['bg_portal_dialog.xml','bg_portal_button.xml']:
 ET.parse(root/'app/src/main/res/drawable'/name)
assert (root/'app/src/main/res/drawable-nodpi/portal_exclamation_red.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
print('Android portal marker hit target and centered confirmation: static checks OK')
