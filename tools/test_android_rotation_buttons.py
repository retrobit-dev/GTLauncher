"""Check that Android's physical button positions retain their own click actions."""
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parent.parent
ns = '{http://schemas.android.com/apk/res/android}'
layout = ET.parse(root / 'app/src/main/res/layout/activity_home.xml')
buttons = {node.get(ns + 'id', '').split('/')[-1]: node for node in layout.iter('ImageButton')}
left, right = buttons['island_turn_left'], buttons['island_turn_right']
assert left.get(ns + 'layout_gravity') == 'left|center_vertical'
assert right.get(ns + 'layout_gravity') == 'right|center_vertical'
assert left.get(ns + 'contentDescription').endswith('ke kanan')
assert right.get(ns + 'contentDescription').endswith('ke kiri')
assert right.get(ns + 'scaleX') == '-1'
activity = (root / 'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'R.id.island_turn_left).setOnClickListener(v->turnGameplayWorld(1))' in activity
assert 'R.id.island_turn_right).setOnClickListener(v->turnGameplayWorld(-1))' in activity
print('Android rotation positions, labels, icons and handlers: OK')
