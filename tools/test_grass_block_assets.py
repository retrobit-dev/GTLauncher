from pathlib import Path
import struct
root=Path(__file__).resolve().parents[1]
preview=(root/'preview/assets/island-grass-detail.png').read_bytes()
android=(root/'app/src/main/res/drawable-nodpi/island_grass_detail.png').read_bytes()
old=(root/'checkpoints/grass-block-before-concept/preview-island-grass-detail.png').read_bytes()
assert preview == android and preview != old
assert preview[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',preview[16:24]) == (1280,160) and preview[25] == 6
icon=(root/'preview/assets/hud/grass-block.png').read_bytes()
assert icon[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',icon[16:24]) == (256,256) and icon[25] == 6
print('Grass block art: shared sparse sheet, original checkpoint and preview thumbnail OK')
