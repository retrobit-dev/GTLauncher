from pathlib import Path
root=Path('app/src/main/java/dev/retrobit/gtlauncher')
home=(root/'HomeActivity.java').read_text();view=(root/'WeatherSkyView.java').read_text();store=(root/'WorldStore.java').read_text();parser=(root/'PreviewWorld20.java').read_text()
assert 'setPositiveButton("Katalog objek"' in home
assert 'GridLayout grid=new GridLayout(this)' in home and 'setText("Tanaman dan pohon")' in home
assert 'placeWildGrassAt(event.getX(),event.getY())' in home
assert 'worldStore.placeWildGrass(id,tile.column,tile.row)' in home
assert 'WorldStore.isPreviewGrass(object)' in home
assert 'R.drawable.wild_grass_selected' in view
assert 'boolean placeWildGrass(String worldId,int column,int row)' in store
assert 'PreviewWorld20.parse(root.toString())' in store
assert 'version>=9 && "wild-grass".equals(kind)' in parser
print('Android wild-grass catalog, placement and safe parser wiring: static checks OK')

assert 'deleteSelectedPlant(worldId,object)' in home and 'setPositiveButton("Hapus"' in home
assert 'skyView.surfaceObjectAt(object,x,y)' in home
assert 'boolean surfaceObjectAt(WorldStore.SurfaceObject o,float sx,float sy)' in view
assert 'boolean deleteSelectedPlant(String worldId,SurfaceObject expected)' in store
