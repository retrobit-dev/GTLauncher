#!/usr/bin/env python3
"""Restore pre-redesign sprites only if neither sprites nor backups have changed."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
snapshot = root / 'checkpoints/character-before-short-stocky'
manifest = json.loads((snapshot / 'manifest.json').read_text())
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
for name, entry in manifest.items():
    target, backup = root / name, snapshot / entry['backup']
    if not target.is_file() or not backup.is_file() or digest(target) != entry['applied'] or digest(backup) != entry['original']:
        raise SystemExit(f'Conflict or missing file: {name}; nothing restored')
for name, entry in manifest.items():
    (root / name).write_bytes((snapshot / entry['backup']).read_bytes())
print('Previous character sprites restored; review and commit the changes.')
