# Rencana rotasi tampilan pulau

Permintaan: putar tampilan pulau setiap 90° ke kiri atau kanan di editor dan gameplay preview serta gameplay Android. Koordinat world, inventaris dan rute yang disimpan tidak berubah. Pilihan kamera bukan migrasi format world.

`preview/world-view-rotation.js` dan `WorldViewRotation.java` menyediakan konversi koordinat kamera empat arah, kebalikannya, arah hadap, tapak persegi panjang, sisi pagar dan tapak jembatan. `projectWorld` di JavaScript dan `WorldViewProjection` di Android juga membentuk salinan world ruang-kamera tanpa mengubah data tersimpan; tes mencakup terrain, ramp, karakter, rumah, pohon, pagar, jembatan dan transparansi. Fungsi picking ruang-kamera juga tersedia dan diuji untuk permukaan, sisi block dan ramp dengan hasil koordinat world. Ini **belum terhubung** ke tombol, renderer, pemilihan, atau gerakan aplikasi.

Integrasi harus menerapkan transformasi yang sama pada urutan kedalaman dan tetangga block, air/air terjun, ketinggian, arah serta sprite player/rumah, pagar, jembatan, tanaman, bayangan, kamera, titik sentuh dan jalur. Konversi balik harus dilakukan sebelum tindakan editor atau gameplay menyentuh data. Jangan memutar world JSON atau inventaris. Uji keempat orientasi pada world bertingkat asimetris, objek, air, seleksi dan pergerakan, termasuk pengembalian ke orientasi awal serta kompatibilitas world lama. Android tetap gameplay saja. Pengujian pada perangkat Android masih diperlukan.

### Experimental preview wiring (not released)

The preview contains an internal `sceneEditing.turnIsland` prototype and hidden controls.
It projects tile painting, terrain neighbor faces, basic sprite facing, object depth,
fences, ramps and tile picking without changing the world records. The controls stay
hidden because this is **not yet a complete player-facing feature**: all editor
selection/drag paths, bridge and grass rendering, occlusion, water/ramp junctions,
and gameplay interaction need end-to-end verification and fixes at each orientation.
The Android renderer and gameplay controls are not connected. Do not enable the
controls until those cases and Android have been implemented and tested. Rotation is
transient and is not part of saved world data.

`tools/test_rotation_integration_browser.js` checks only a small preview smoke test:
four turns, selected tile centres, round-trip picking and unchanged world data. It
does not certify the interactions listed above.
