from pathlib import Path
view=Path('app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
store=Path('app/src/main/java/dev/retrobit/gtlauncher/WorldStore.java').read_text()
reader=Path('app/src/main/java/dev/retrobit/gtlauncher/PreviewWorld20.java').read_text()
home=Path('app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'ElevatedTerrainGeometry.tileAt(' in view
assert 'for(int level=0;level<height;level++)' in view and 'worldTerrain.block(column,row,level)' in view
assert 'drawWaterfalls(canvas,column,row' in view and 'StackWater.atLevel' in view and 'StackWater.fall' in view
for s in ['elevationAt(o.column,o.row)','elevationAt(house.column,house.row)','elevationAt(b.column,b.row)','-visualElevation()']:
    assert s in view
assert 'terrainHeight(pair.a.column,pair.a.row)!=terrainHeight(pair.b.column,pair.b.row)' in view
assert 'FenceJoins.plan(worldSurfaces,o->terrainHeight(o.column,o.row))' in view
assert 'worldTerrain::height' in view
assert 'data.has("blockStacks")' in store and 'stack.put(stack.length()-1,6)' in store
assert 'Older preview format cannot replace stacked terrain' in store
assert 'return new PreviewWorld20(root.toString(),id,size,version,terrain,bridges,opacityByBlock,directions,materials)' in reader
assert 'drawRamp(canvas,' in view and 'worldTerrain::rampDirection' in view
assert 'RampGeometry.stepHeight(' in view and 'RampGeometry.canConnect(' in reader
assert 'Older preview format cannot replace ramps' in store
assert 'MAX_WORLD_BYTES = 16 * 1024 * 1024' in home
print('Android stacked terrain rendering, object anchors, picking, water, shadow, camera and persistence wiring: static checks OK')
