from pathlib import Path
import hashlib, struct
root=Path(__file__).resolve().parents[1]
for side in ('front','back'):
    checksums=[]
    for i in range(12):
        preview=(root/f'preview/assets/character-walk/{side}-{i:02}.png').read_bytes()
        android=(root/f'app/src/main/res/drawable-nodpi/character_walk_{side}_{i:02}.png').read_bytes()
        assert preview==android
        assert preview[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',preview[16:24])==(216,400) and preview[25]==6
        checksums.append(hashlib.sha256(preview).digest())
    assert checksums[0]!=checksums[3] and checksums[3]!=checksums[9]
print('Twelve transparent frames per view, alternating poses and Android parity: OK')
