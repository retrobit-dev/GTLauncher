# Semak

Konsep semak pilihan kedua disimpan di `preview/assets/character-redraw/bush-concept.png`. `tools/build_bush_selected.py` mengekstrak sprite transparan yang sama untuk preview dan Android; gambar semak Android lama tetap dipertahankan untuk world native lama.

Preview: Map Editor → Objek → Tanaman dan pohon → Semak. Tapaknya 1×1 pada tile kering yang kosong. Semak memiliki gerakan angin, tabrakan, bayangan kontak kecil, pemilihan siluet, pindah, putar, hapus, Undo/Redo dan penyimpanan. Semak tidak menghasilkan jeruk atau loot.

Android: World → Katalog objek → Tanaman dan pohon → Semak pada world preview datar dengan karakter. Alat tangan dapat memindahkan atau menghapus semak preview yang berada di depan karakter. Hapus memerlukan konfirmasi, tidak menambah tas, dan belum mendukung Undo di Android. Penyimpanan preview dan Android terpisah.

Preview menyimpan semak pada daftar `orangeTrees` yang sudah ada dengan `kind:"bush",size:1`; JSON yang memuat semak memakai versi 10. Android mendukung hanya subset world datar v10 berisi semak, rumput liar dan/atau cemara; world bertingkat atau berpohon jeruk tetap ditolak tanpa mengubah data. Impor versi lama tidak dapat mengganti world yang memiliki semak. Build dan pengujian perangkat Android belum dilakukan.
