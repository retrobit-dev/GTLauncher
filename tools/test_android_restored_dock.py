from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parent.parent
ns = '{http://schemas.android.com/apk/res/android}'
layout = ET.parse(root/'app/src/main/res/layout/activity_home.xml').getroot()
ids = {n.get(ns+'id'):n for n in layout.iter() if n.get(ns+'id')}
assert ids['@+id/home_dock_scroll'].tag == 'HorizontalScrollView'
assert ids['@+id/home_dock_scroll'].get(ns+'alpha') == '0.85'
assert all(node.get(ns+'alpha') is None for node in ids['@+id/home_dock_scroll'].iter() if node is not ids['@+id/home_dock_scroll'])
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
    elif name in ('hoe','shovel','water','sickle','axe','pickaxe','fishing'):
        import struct
        png=(root/f'app/src/main/res/drawable-nodpi/dock_tool_{name}.png').read_bytes()
        assert png==(root/f'preview/assets/hud/tools/{name}-selected.png').read_bytes()
        assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(512,512) and png[25]==6
    else:
        ET.parse(root/'app/src/main/res/drawable'/('dock_tool_'+name+'.xml'))
import struct
settings=(root/'app/src/main/res/drawable-nodpi/dock_settings.png').read_bytes()
assert settings==(root/'preview/assets/hud/dock_settings.png').read_bytes()
assert settings[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',settings[16:24])==(512,512) and settings[25]==6
tab=ids['@+id/dock_tools_tab']
assert tab.tag=='TextView' and tab.get(ns+'text')=='Tools' and tab.get(ns+'contentDescription')=='Tools'
assert tab.get(ns+'layout_width') == ids['@+id/dock_tool_hand'].get(ns+'layout_width')
assert tab.get(ns+'textStyle') == ids['@+id/dock_add_tab'].get(ns+'textStyle') == 'bold'
route=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text().split('private void drawGameplayRoute(Canvas canvas)',1)[1].split('private void drawHoeTarget',1)[0]
assert route.count('setStrokeWidth(')==1 and 'setStrokeWidth(.5f)' in route
assert 'canvas.drawPath(path,paint)' in route and 'canvas.drawPath(diamond,paint)' in route
selected=ET.parse(root/'app/src/main/res/drawable/bg_dock_hand.xml').getroot()[0]
assert selected.find('shape/solid').get(ns+'color')=='#FFF3A6'
assert selected.find('shape/stroke').get(ns+'color')=='#17130E'
# 54dp clock stack at 0.75 scale is 40.5dp; the dock row matches it.
assert ids['@+id/dock_tool_hand'].get(ns+'layout_height')=='40.5dp'
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
