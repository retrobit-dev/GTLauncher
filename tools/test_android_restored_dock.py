from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parent.parent
ns = '{http://schemas.android.com/apk/res/android}'
layout = ET.parse(root/'app/src/main/res/layout/activity_home.xml').getroot()
ids = {n.get(ns+'id'):n for n in layout.iter() if n.get(ns+'id')}
assert ids['@+id/home_dock_scroll'].tag == 'HorizontalScrollView'
keys = ('hand','hoe','shovel','water','sickle','axe','pickaxe','fishing')
for name in keys:
    node=ids['@+id/dock_tool_'+name]
    assert node.tag=='ImageButton' and node.get(ns+'src')=='@drawable/dock_tool_'+name
    assert ('belum tersedia' in node.get(ns+'contentDescription')) == (name != 'hand')
    if name == 'hoe':
        assert (root/'app/src/main/res/drawable-nodpi/dock_tool_hoe.png').read_bytes() == (root/'preview/assets/hud/tools/hoe-selected.png').read_bytes()
    else:
        ET.parse(root/'app/src/main/res/drawable'/('dock_tool_'+name+'.xml'))
assert '@+id/dock_add_tab' in ids
assert '@+id/dock_close' not in ids
java=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
for name in keys: assert 'R.id.dock_tool_'+name in java
assert 'Fungsi alat belum tersedia.' in java
assert 'findViewById(R.id.dock_tool_hand).setSelected(true)' in java
assert 'findViewById(R.id.dock_tool_hoe).setOnClickListener' in java
assert 'worldStore.tillTile(world.id,target.column,target.row)' not in java
assert 'boolean wasSelected=value?hoeSelected:handSelected' in java
assert 'skyView.setHoeSelected(hoeSelected)' in java
assert ids['@+id/dock_tool_hand'].get(ns+'background')=='@drawable/bg_dock_hand'
ET.parse(root/'app/src/main/res/drawable/bg_dock_hand.xml')
print('Android restored dock controls, vector icons and unavailable-action disclosure: static OK')
