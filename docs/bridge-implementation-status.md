# Bridge implementation status

The approved wooden and stone artwork are concepts, not placeable objects yet. The current 3-tile geometry helpers are standalone and not used by editor, renderer, save/import or pathfinder. No existing world or terrain schema has been changed.

The placement rules under test reject out-of-bounds footprints, overlapping bridges, missing or unequal-height ground, water at either end, and collisions with houses, plants and fences (including fences represented from the neighboring tile). The middle tile can be water. Traversal rules allow entering only at the two ends. The Java helper accepts an obstruction callback; the Android caller must check the same collision cases as preview before using it.

Still required before making bridges available in Bangunan: implement and version a lossless bridge schema in both apps, validate all terrain and object edits against bridge footprints, render both approved designs in both axes and handle occlusion, integrate movement and idle/water behavior, add placement/rotation/selection/deletion/undo and repeat placement, and test export/import/downgrade safety. Android currently imports only flat supported preview worlds; stacks must continue to be rejected rather than flattened. Do not expose a placement button until these are connected.
