from pathlib import Path
home = Path('app/src/main/java/dev/retrobit/gtlauncher/HomeActivity.java').read_text()
dialog = home[home.index('private void showWorldDialog()'):home.index('private void showObjectCatalog()')]
assert 'chooseWorldFile()' in dialog
assert 'showObjectCatalog()' not in dialog
assert 'setSingleChoiceItems' in dialog
# The only remaining callers are refresh callbacks inside the now-unreachable catalog itself.
assert home.count('showObjectCatalog()') == 4
assert 'catalog-place-house' in Path('preview/index.html').read_text()
print('Android world picker retains import and has no catalog entry; preview editor retained: static checks OK')
