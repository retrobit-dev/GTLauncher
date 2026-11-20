#!/usr/bin/env python3
"""Seal a redraw and restore its pre-redraw files only if none changed since sealing."""
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SNAP = ROOT / 'checkpoints/character-before-redraw'
FILES = {
    'preview/app.js': 'preview/app.js',
    'preview/assets/main-character.png': 'preview/main-character.png',
    'preview/assets/main-character-back.png': 'preview/main-character-back.png',
    'app/src/main/java/dev/retrobit/gtlauncher/WeatherSkyView.java': 'android/WeatherSkyView.java',
    'app/src/main/res/drawable-nodpi/character_original_uhd_front.png': 'android/character_original_uhd_front.png',
    'app/src/main/res/drawable-nodpi/character_original_uhd_back.png': 'android/character_original_uhd_back.png',
}

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    if len(sys.argv) != 2 or sys.argv[1] not in ('seal', 'undo'):
        sys.exit('Usage: python3 tools/character_redraw_checkpoint.py seal|undo')
    seal = SNAP / 'sealed.json'
    if sys.argv[1] == 'seal':
        if seal.exists(): sys.exit('Already sealed; refusing to replace the protection.')
        seal.write_text(json.dumps({p: digest(ROOT / p) for p in FILES}, indent=2)+'\n')
        print('Redraw state sealed.')
        return
    if not seal.exists(): sys.exit('Not sealed; nothing is ready to undo.')
    hashes = json.loads(seal.read_text())
    if set(hashes) != set(FILES): sys.exit('Checkpoint file list mismatch; refusing to restore.')
    baseline = json.loads((SNAP / 'baseline.json').read_text())
    if set(baseline) != set(FILES): sys.exit('Baseline file list mismatch; refusing to restore.')
    for name, old in FILES.items():
        path, backup = ROOT / name, SNAP / old
        if not path.is_file() or digest(path) != hashes[name] or not backup.is_file() or digest(backup) != baseline[name]:
            sys.exit(f'Conflict or missing backup: {name}; nothing restored.')
    for name, old in FILES.items():
        (ROOT / name).write_bytes((SNAP / old).read_bytes())
    seal.unlink()
    print('Pre-redraw files restored. Review and remove any newly added assets separately.')

if __name__ == '__main__': main()
