# Rencana rotasi tampilan pulau

Permintaan: putar tampilan pulau setiap 90° ke kiri atau kanan di editor dan gameplay preview serta gameplay Android. Koordinat world, inventaris dan rute yang disimpan tidak berubah. Pilihan kamera bukan migrasi format world.

`preview/world-view-rotation.js` dan `WorldViewRotation.java` menyediakan konversi koordinat kamera empat arah, kebalikannya, arah hadap, tapak persegi panjang, sisi pagar dan tapak jembatan. `projectWorld` di JavaScript dan `WorldViewProjection` di Android juga membentuk salinan world ruang-kamera tanpa mengubah data tersimpan; tes mencakup terrain, ramp, karakter, rumah, pohon, pagar, jembatan dan transparansi. Fungsi picking ruang-kamera juga tersedia dan diuji untuk permukaan, sisi block dan ramp dengan hasil koordinat world. Integrasi preview masih bersifat eksperimental dan tombolnya disembunyikan. Android sekarang memiliki integrasi terbatas untuk world preview berkarakter tanpa objek, rumah, portal, atau jembatan; lihat batasan di bawah.

Integrasi harus menerapkan transformasi yang sama pada urutan kedalaman dan tetangga block, air/air terjun, ketinggian, arah serta sprite player/rumah, pagar, jembatan, tanaman, bayangan, kamera, titik sentuh dan jalur. Konversi balik harus dilakukan sebelum tindakan editor atau gameplay menyentuh data. Jangan memutar world JSON atau inventaris. Uji keempat orientasi pada world bertingkat asimetris, objek, air, seleksi dan pergerakan, termasuk pengembalian ke orientasi awal serta kompatibilitas world lama. Android tetap gameplay saja. Pengujian pada perangkat Android masih diperlukan.

### Experimental preview wiring (not released)

The preview contains an internal `sceneEditing.turnIsland` prototype and hidden controls.
It projects tile painting, terrain neighbor faces, basic sprite facing, object depth,
fences, ramps and tile picking without changing the world records. The controls stay
hidden because this is **not yet a complete player-facing feature**: all editor
selection/drag paths, bridge and grass rendering, occlusion, water/ramp junctions,
and gameplay interaction need end-to-end verification and fixes at each orientation.
Do not enable the preview controls until those cases have been implemented and tested. Rotation is
transient and is not part of saved world data.

`tools/test_rotation_integration_browser.js` checks only a small preview smoke test:
four turns, selected tile centres, round-trip picking and unchanged world data. It
does not certify the interactions listed above.


### Verification of the experimental preview

`tools/test_rotation_interactions_browser.js` exercises all four orientations in
an asymmetric 50×50 stacked world. It checks a raised top and level selection,
ramp and fence/bridge picking, the tree footprint centre, overlapping tree
artwork picking against painter order, the house alpha hit test, an isolated
water border, unchanged world/storage when turning, real pointer taps replacing
four blocks, and simple adjacent-tile gameplay taps with reduced motion. It then
reloads to check the edits and player position persist while camera rotation
returns to zero. These checks use the internal prototype, not the hidden buttons.

`tools/test_rotation_depth_water_browser.js` additionally checks overlapping
fence face picking in all directions and camera-side water borders/waterfalls
for two unequal-height water neighbours. The overlapping-object checks exposed
that picking previously followed reverse storage order rather than the visible
painter order; tree and fence picking now use their respective draw depths, with
stable array order for ties. These checks do not test overlap *between different*
object categories, actual rendered waterfall/ramp joins, or all face/level cases.

The feature is still not ready to expose. In particular, verify visual rendering
and occlusion (including bridges, grass, house and shadows), object/area selection
and drag, ramps with water, moving-camera transitions and non-reduced-motion
path following in all orientations. Android has limited gameplay integration as described below,
but has not been tested as an APK on a device or emulator.


`tools/test_rotation_group_browser.js` sends pointer events in all four camera
orientations to select a grass-and-fence group by area, move the group to a
world tile, and undo. The test uses synthetic pointer events in Chromium; it
does not certify physical touch, individual object drag, overlapping categories,
or rotation while a gesture or an animated route is in progress.

New preview worlds now start with independent 50×50 copies of the Base World
five-block stacks. `tools/test_new_world_from_base_browser.js` also checks
picking those stacks in all four experimental orientations, erasing one top
block at a rotated view, undo/redo, version 4 export and reload without changing
old worlds or inventory. This does not enable the hidden rotation controls.


### Android: limited gameplay integration

Imported preview worlds with a visible player and **no houses, surface objects,
portals or bridges** expose left/right rotation buttons. Other worlds remain in
the original orientation and do not show those controls, avoiding unsafe object or
portal hit-testing. Legacy native worlds and Base World are not included. The
rotation is view-only and resets when switching worlds or recreating the view.

For the supported subset, an isolated rotated terrain copy feeds the existing
renderer, water, ramp and terrain picking; taps map back to world coordinates.
Character facing/steps, route overlays, camera following and the idle-water
callback are mapped between world and view coordinates. The buttons reject turns
while a route/step is active; stored world JSON and inventory are not rewritten.
Projected terrain is cached by source JSON and orientation.

`WorldViewProjectionTest.gameplayTerrainCopyPreservesWorldAndMovement` checks
four-way terrain/material/opacity/ramp mapping, picking, interpolated steps,
route edge equivalence and source JSON preservation in plain JVM tests. Full
Android Java was typechecked against an API stub and the layout parsed as XML.
This is not an APK build, an instrumented test or a device/emulator test. Do
not infer that rendering, touch, animation, performance, water/ramp junctions,
or idle-water behavior were verified on Android. To support worlds with
objects, future work must rotate their render order, footprints, facing, shadows,
identifiers and hit results back to original records, including native portals,
roads and bridges, and test those interactions on a device.
