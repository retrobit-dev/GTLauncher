#!/usr/bin/env python3
"""Draft 2D run-cycle joint guides. Coordinates are sprite-space, not game frames.

The two views share timing and limb lengths.  These guides do not extract or
render artwork; independent limb drawings and seam/occlusion work remain needed.
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'preview/assets/character-redraw/run-rig/joints.json'

# 216x400 sprite coordinate system; anchors are illustrative, not an automatic mask.
ANCHORS = {
    'front': {'near_shoulder': (148, 190), 'far_shoulder': (68, 190),
              'near_hip': (119, 290), 'far_hip': (91, 290)},
    'back':  {'near_shoulder': (150, 190), 'far_shoulder': (64, 190),
              'near_hip': (122, 290), 'far_hip': (91, 290)},
}

def segment(origin, length, angle, direction):
    return (origin[0] + direction * length * math.sin(angle),
            origin[1] + length * math.cos(angle))

def joints(view, phase):
    if view not in ANCHORS:
        raise ValueError(view)
    d = -1 if view == 'front' else 1
    result = {}
    for side, sign in (('near', 1), ('far', -1)):
        s = sign * math.sin(phase)
        hip = ANCHORS[view][side + '_hip']
        shoulder = ANCHORS[view][side + '_shoulder']
        # Opposing thighs and arms; trailing knee folds during the recovery.
        thigh = .72 * s
        knee = segment(hip, 43, thigh, d)
        shin = thigh - .85 * max(0, -s)
        ankle = segment(knee, 38, shin, d)
        arm = -.68 * s
        elbow = segment(shoulder, 29, arm, d)
        forearm = arm + .95
        wrist = segment(elbow, 24, forearm, d)
        result[side] = {k: [round(p[0], 3), round(p[1], 3)] for k,p in
                        (('hip', hip), ('knee', knee), ('ankle', ankle),
                         ('shoulder', shoulder), ('elbow', elbow), ('wrist', wrist))}
    return result

def main():
    data = {'status': 'joint-guide-only; no layered artwork or runtime integration',
            'canvas': [216, 400], 'cycle_frames': 12,
            'views': {view: [joints(view, 2*math.pi*i/12) for i in range(12)]
                      for view in ANCHORS}}
    OUT.write_text(json.dumps(data, indent=2) + '\n')

if __name__ == '__main__':
    main()
