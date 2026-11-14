from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'drawPortalMarkers(scene);' in view
assert 'portalMarkerVisible(portal) && hit.contains(x,y)' in view
assert '!portalWalkActive && showCharacter && characterColumn==portal.tile.column && characterRow==portal.tile.row && portal.edge.equals(characterFacing)' in view
assert 'portalMarkerAt(event.getX(),event.getY())' in activity
assert 'if(marker!=null) confirmPortalMarker(marker)' in activity
assert 'setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)' in activity and 'FLAG_DIM_BEHIND' in activity
assert 'R.drawable.character_hud_portrait' in activity and 'name.setText("Portal")' in activity
assert 'portrait.setScaleX(-1f)' in activity and 'portrait.setBackgroundResource(R.drawable.bg_character_hud)' in activity
assert 'worldStore.travel(sourceId,portalId)' in activity
assert 'no.setOnClickListener(v->dialog.dismiss())' in activity
assert 'sellingMarkerAt(event.getX(),event.getY())' in activity and 'showSellingPlaceholder(selling)' in activity
assert 'Math.abs(characterColumn-o.column)+Math.abs(characterRow-o.row)==1' in view
assert 'drawSellingMarkers(scene);' in view and 'setSellingOpen(box,false)' in activity
assert '@font/press_start_2p' in (root/'app/src/main/res/layout/activity_home.xml').read_text()
for name in ['bg_portal_dialog.xml','bg_portal_button.xml']:
 ET.parse(root/'app/src/main/res/drawable'/name)
assert (root/'app/src/main/res/drawable-nodpi/portal_exclamation_red.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
print('Android portal marker hit target and adventure-style confirmation: static checks OK')

assert 'cx-ISLAND_TILE_WIDTH*.5f,cy-ISLAND_TILE_HEIGHT*.5f' in view
portal_draw=view.split('private void drawPortals(',1)[1].split('private RectF portalMarkerBounds(',1)[0]
assert 'Paint.Style.STROKE' in portal_draw and 'canvas.drawPath(outline,paint)' in portal_draw
assert 'outline.lineTo(g.right,g.centerY())' in portal_draw and 'outline.lineTo(g.left,g.centerY())' in portal_draw
assert 'drawBitmap' not in portal_draw and 'canvas.rotate' not in portal_draw
assert 'Math.abs(dx)+Math.abs(dy)<=1f' in view
