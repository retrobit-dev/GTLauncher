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
    assert node.get(ns+'background')=='@drawable/bg_dock_hand'
    if name == 'hand':
        import struct
        png=(root/'app/src/main/res/drawable-nodpi/dock_tool_hand.png').read_bytes()
        assert png==(root/'preview/assets/hud/tools/glove-selected.png').read_bytes()
        assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(512,512) and png[25]==6
    elif name == 'hoe':
        import struct
        png=(root/'app/src/main/res/drawable-nodpi/dock_tool_hoe.png').read_bytes()
        assert png==(root/'preview/assets/hud/tools/hoe-selected.png').read_bytes()
        assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(512,512) and png[25]==6
    else:
        ET.parse(root/'app/src/main/res/drawable'/('dock_tool_'+name+'.xml'))
assert '@+id/dock_add_tab' in ids
assert '@+id/dock_close' not in ids
java=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
for name in keys: assert 'R.id.dock_tool_'+name in java
assert 'Fungsi alat ini belum tersedia.' in java
assert 'private int selectedTool = -1;' in java
assert 'findViewById(R.id.dock_tool_hand).setSelected(true)' not in java
assert 'skyView.setHoeSelected(true)' not in java
assert 'for(int tool: gameplayTools())' in java
assert 'worldStore.tillTile(world.id,target.column,target.row)' not in java
assert 'selectedTool=selectedTool==tool ? -1 : tool' in java
assert 'skyView.setHoeSelected(selectedTool==R.id.dock_tool_hoe || selectedTool==R.id.dock_tool_hand)' in java
assert 'GameplayTarget.direction(route.get(0),route.get(1))' in java
assert ids['@+id/dock_tool_hand'].get(ns+'background')=='@drawable/bg_dock_hand'
ET.parse(root/'app/src/main/res/drawable/bg_dock_hand.xml')
print('Android restored dock controls, vector icons and unavailable-action disclosure: static OK')
