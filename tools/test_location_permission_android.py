"""Static checks only; Android permission and settings flows require device tests."""
from pathlib import Path
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent
ns='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(r/'app/src/main/AndroidManifest.xml').getroot()
permissions={v.get(ns+'name') for v in manifest.findall('uses-permission')}
assert {'android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_COARSE_LOCATION'} <= permissions
layout=ET.parse(r/'app/src/main/res/layout/activity_home.xml').getroot()
button=layout.find(".//*[@"+ns+"id='@+id/location_permission_button']")
assert button is not None and button.get(ns+'clickable')=='true' and button.get(ns+'focusable')=='true'
strings=ET.parse(r/'app/src/main/res/values/strings.xml').getroot()
values={v.get('name'):''.join(v.itertext()) for v in strings.findall('string')}
assert 'Open-Meteo' in values['location_permission_explanation'] and 'Rembang' in values['location_permission_explanation']
s=(r/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
for fragment in ['location_permission_explanation_shown','showLocationPermissionExplanation()', 'location_permission_button).setOnClickListener', 'shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)', 'Settings.ACTION_APPLICATION_DETAILS_SETTINGS', 'Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION', 'onRequestPermissionsResult', 'fetchWeather(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true)']:
 assert fragment in s,fragment
print('Android permission explanation, initial request, settings retry and fallback: static checks OK')
