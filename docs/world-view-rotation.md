# Rencana rotasi tampilan pulau

Permintaan: putar tampilan pulau setiap 90° ke kiri atau kanan di editor dan gameplay preview serta gameplay Android. Koordinat world, inventaris dan rute yang disimpan tidak berubah. Pilihan kamera bukan migrasi format world.

`preview/world-view-rotation.js` dan `WorldViewRotation.java` menyediakan konversi koordinat kamera empat arah, kebalikannya, arah hadap, tapak persegi panjang, sisi pagar dan tapak jembatan. `projectWorld` di JavaScript dan `WorldViewProjection` di Android juga membentuk salinan world ruang-kamera tanpa mengubah data tersimpan; tes mencakup terrain, ramp, karakter, rumah, pohon, pagar, jembatan dan transparansi. Fungsi picking ruang-kamera juga tersedia dan diuji untuk permukaan, sisi block dan ramp dengan hasil koordinat world. Integrasi preview masih bersifat eksperimental; tombolnya ditampilkan pada gameplay dan editor atas permintaan pengguna. Android menampilkan tombol di setiap world dan memiliki integrasi untuk world impor preview, termasuk rumah 3×3, tanaman, pagar, dan jembatan; world Android lama dan portal masih belum mendukung rotasi.

Integrasi harus menerapkan transformasi yang sama pada urutan kedalaman dan tetangga block, air/air terjun, ketinggian, arah serta sprite player/rumah, pagar, jembatan, tanaman, bayangan, kamera, titik sentuh dan jalur. Konversi balik harus dilakukan sebelum tindakan editor atau gameplay menyentuh data. Jangan memutar world JSON atau inventaris. Uji keempat orientasi pada world bertingkat asimetris, objek, air, seleksi dan pergerakan, termasuk pengembalian ke orientasi awal serta kompatibilitas world lama. Android tetap gameplay saja. Pengujian pada perangkat Android masih diperlukan.

### Experimental preview wiring

The preview exposes the `sceneEditing.turnIsland` prototype through visible controls in gameplay and editor.
It projects tile painting, terrain neighbor faces, basic sprite facing, object depth,
fences, ramps and tile picking without changing the world records. This is
**experimental**: editor selection/drag paths, bridge and grass rendering,
occlusion, water/ramp junctions, and gameplay interactions still need additional
end-to-end verification and fixes at each orientation. Rotation is transient and
is not part of saved world data. `tools/test_rotation_buttons_browser.js` checks
that both buttons are visible and work in gameplay and editor without changing
the active world.

`tools/test_rotation_integration_browser.js` checks only a small preview smoke test:
four turns, selected tile centres, round-trip picking and unchanged world data. It
does not certify the interactions listed above.


### Verification of the experimental preview

Re-run after exposing the controls: button visibility/turns, integration, group
selection/move/undo, overlapping fences and water neighbours, and the four-way
interaction/reload browser suites passed. The interaction test uses a longer
DOM-ready reload wait to avoid timing out on slow rendering. These tests do not
cover physical touch, every object combination, active gestures/routes, or
visual correctness on all devices.

`tools/test_rotation_interactions_browser.js` exercises all four orientations in
an asymmetric 50×50 stacked world. It checks a raised top and level selection,
ramp and fence/bridge picking, the tree footprint centre, overlapping tree
artwork picking against painter order, the house alpha hit test, an isolated
water border, unchanged world/storage when turning, real pointer taps replacing
four blocks, and simple adjacent-tile gameplay taps with reduced motion. It then
reloads to check the edits and player position persist while camera rotation
returns to zero. These checks exercise the underlying prototype rather than the buttons.

`tools/test_rotation_depth_water_browser.js` additionally checks overlapping
fence face picking in all directions and camera-side water borders/waterfalls
for two unequal-height water neighbours. The overlapping-object checks exposed
that picking previously followed reverse storage order rather than the visible
painter order; tree and fence picking now use their respective draw depths, with
stable array order for ties. These checks do not test overlap *between different*
object categories, actual rendered waterfall/ramp joins, or all face/level cases.

The feature remains experimental. In particular, verify visual rendering
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
old worlds or inventory.


### Android: gameplay rotation

The left/right buttons are visible on all worlds. Imported preview worlds can
rotate whether or not the player is visible, including their 3×3 house,
preview plants/trees, fences and bridges. Unsupported native/legacy worlds,
Base World and portals retain their original orientation and the buttons show
a message explaining that rotation is not yet supported. A turn is rejected
while a route/step is in progress. Rotation is transient and resets when the
world or view is recreated; stored world JSON and inventory are not rewritten.

Camera-space terrain copies drive rendering, water, ramps and picking. Player
steps, route overlays and idle-water callbacks are converted between camera and
world coordinates. The renderer uses camera-space house and preview-object
copies, but hit results, plant harvest identities and storage operations refer to
the original records. Object sway uses the original coordinates. Terrain copies
are cached by source JSON and orientation.

Plain JVM tests exercise four orientations of a full preview import with house,
plants, fence, bridge, water, ramp and opacity; they check projection round trips,
source/harvest identity, terrain picking and route-edge equivalence. All Android
Java was typechecked against an API stub and the layout was parsed as XML. This
is not an APK build, an instrumented test or a device/emulator test. Rendering,
touch, animation, performance, house/object interactions, water/ramp junctions
and idle-water behavior still need verification on Android. Native/legacy worlds
and portals require their own rendering, footprint, facing, depth, shadow and
hit mapping before their buttons can rotate them safely.
