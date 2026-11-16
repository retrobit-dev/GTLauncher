from pathlib import Path
import hashlib, json, struct
root=Path(__file__).resolve().parents[1]
checkpoint=root/'checkpoints/character-before-backpack'
old=json.loads((checkpoint/'baseline.json').read_text()); applied=json.loads((checkpoint/'applied.json').read_text())
assert set(old)==set(applied)
for name,entry in old.items():
    assert hashlib.sha256((checkpoint/entry['backup']).read_bytes()).hexdigest()==entry['sha256']
for preview,android in [('main-character.png','character_original_uhd_front.png'),('main-character-back.png','character_original_uhd_back.png')]:
    data=(root/'preview/assets'/preview).read_bytes()
    assert data==(root/'app/src/main/res/drawable-nodpi'/android).read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(432,800) and data[25]==6
for direction in ('n','ne','e','se','s','sw','w','nw'):
    assert (root/f'preview/assets/character-redraw/directions/{direction}.png').is_file()
assert 'const PLAYER_WIDTH = 13.5;' in (root/'preview/app.js').read_text()
assert 'const PLAYER_HEIGHT = 25;' in (root/'preview/app.js').read_text()
print('Historical checkpoint, current transparent source dimensions, Android parity and eight reference files: OK')
