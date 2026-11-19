from pathlib import Path
view=Path('app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
assert 'HouseShadow.projection(width,height,light.azimuthDegrees,light.altitudeDegrees)' in view
assert 'CharacterShadow.fitAroundAnchor(width,height,ray[0],ray[1])' in Path('app/src/main/java/dev/retrobit/gtlauncher/HouseShadow.java').read_text()
assert 'float shadowScale=CharacterShadow.fitScale(width,height,dx+sway,dy);' in view
assert 'baseWidth/(width+2*Math.abs(ray[0]))' in view
assert 'tree.material.equals("conifer-small")?.5:1' in view
assert 'drawObjectContact(canvas,x,y,70*scale*(wide?7f/6:1),ISLAND_TILE_HEIGHT*2.5f*scale)' in view
print('Android house, tree and character shadow-bound wiring: static checks OK')
