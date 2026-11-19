# Semak

Konsep semak polos pilihan kedua disimpan di `preview/assets/character-redraw/bush-concept.png`. Konsep semak bunga dan beri pilihan juga disimpan di folder yang sama. `tools/build_bush_selected.py` mengekstrak sprite transparan yang sama untuk preview dan Android; gambar semak Android lama tetap dipertahankan untuk world native lama.

Preview: Map Editor → Objek → Tanaman dan pohon → Semak. Semak polos, bunga dan beri masing-masing tersedia dengan tapak 1×1 dan 2×2 pada tile kering yang kosong dan rata. Semak memiliki gerakan angin, tabrakan, bayangan kontak kecil, pemilihan siluet, pindah, putar, hapus, Undo/Redo dan penyimpanan. Bunga dan beri masih dekoratif: semak tidak menghasilkan jeruk, loot atau hasil panen.

Android: World → Katalog objek → Tanaman dan pohon → Semak pada world preview datar dengan karakter. Alat tangan dapat memindahkan atau menghapus semak preview yang berada di depan karakter. Hapus memerlukan konfirmasi, tidak menambah tas, dan belum mendukung Undo di Android. Penyimpanan preview dan Android terpisah.

Preview menyimpan semak pada daftar `orangeTrees` yang sudah ada dengan `kind:"bush"`, ukuran `size:1` atau `size:2`, dan `variant` opsional untuk bunga/beri; JSON semak polos 1×1 memakai versi 10; ukuran 2×2 dan variasi bunga/beri memakai versi 11. Android mendukung hanya subset world datar v10/v11 berisi semak, rumput liar dan/atau cemara; world bertingkat atau berpohon jeruk tetap ditolak tanpa mengubah data. Impor versi lama tidak dapat mengganti world yang memiliki semak. Build dan pengujian perangkat Android belum dilakukan.
