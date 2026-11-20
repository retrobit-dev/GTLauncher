from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
java = (root / 'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
assert 'pixelArtIcon(resolveInfo.loadIcon(packageManager))' in java
assert 'final int pixels = 24;' in java
assert 'pixelIcon.setFilterBitmap(false)' in java
assert 'Color.alpha(color)' in java
assert 'return original;' in java
assert 'iconView.setBackgroundResource(R.drawable.bg_pixel_app_icon)' in java
assert 'setComponent(new ComponentName(app.packageName, app.activityName))' in java
frame = ET.parse(root / 'app/src/main/res/drawable/bg_pixel_app_icon.xml').getroot()
assert frame.tag == 'layer-list' and len(frame) == 3
print('Android pixel icons: rasterization, crisp scaling, fallback, frame and launch preserved (static checks): OK')
