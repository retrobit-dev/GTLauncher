from pathlib import Path
root=Path(__file__).resolve().parents[1]
store=(root/'app/src/main/java/dev/retrobit/gtlauncher/WorldStore.java').read_text()
activity=(root/'app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
# Until Android can preserve and render every object, reject the new portable format.
assert 'version != 27.0 && version != 28.0 && version != 29.0) throw new JSONException("Unsupported world version")' in store
assert 'if (!variant.isEmpty() && !variant.equals("portal")) throw new JSONException("Unsupported object")' in store
assert '"Unsupported world version".equals(error.getMessage())' in activity
assert 'Versi world ini belum didukung Android. Data tidak diubah.' in activity
print('Android validates supported portals and rejects unsupported objects without modifying data: static check OK')
