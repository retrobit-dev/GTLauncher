#!/usr/bin/env python3
"""Restore the exact pre-backpack sprites only if the applied images were not edited."""
import hashlib, json, sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
backup=root/'checkpoints/character-before-backpack'
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
old=json.loads((backup/'baseline.json').read_text())
applied=json.loads((backup/'applied.json').read_text())
if set(old)!=set(applied): sys.exit('Checkpoint mismatch; nothing restored.')
for name, info in old.items():
    current=root/name; saved=backup/info['backup']
    if not current.is_file() or not saved.is_file() or sha(current)!=applied[name] or sha(saved)!=info['sha256']:
        sys.exit(f'Conflict or missing backup: {name}; nothing restored.')
for name, info in old.items(): (root/name).write_bytes((backup/info['backup']).read_bytes())
print('Sprites restored; review changes and commit them.')
