from pathlib import Path
root=Path(__file__).resolve().parents[1]
store=(root/'app/src/main/java/dev/retrobit/gtlauncher/WorldStore.java').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
view=(root/'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java').read_text()
assert 'o.material.equals("wild-grass")?10f:18.5f' in view and 'o.material.equals("wild-grass")?8f:14.5f' in view
# Until Android can preserve and render every object, reject the new portable format.
assert 'version != 27.0 && version != 28.0 && version != 29.0 && version != 30.0) throw new JSONException("Unsupported world version")' in store
assert 'if (!variant.isEmpty() && !variant.equals("portal")) throw new JSONException("Unsupported object")' in store
assert '"Unsupported world version".equals(error.getMessage())' in activity
assert 'Versi world ini belum didukung Android. Data tidak diubah.' in activity
print('Android validates supported portals and rejects unsupported objects without modifying data: static check OK')
