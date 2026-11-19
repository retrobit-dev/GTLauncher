# Rumput liar

Konsep rumput liar yang dipilih disimpan sebagai `preview/assets/character-redraw/wild-grass-new-concept.png`. `tools/build_wild_grass_selected.py` menghasilkan sprite transparan dengan proporsi asli dan titik pijak di bawah. Gambar lama Android `wild_grass.png` tetap disimpan; gambar terpilih digunakan oleh renderer Android untuk objek `bush` varian `wild-grass` yang sudah didukung dalam world lama. Ukuran kotak renderer dan penyimpanan world tidak diubah.

Sprite baru juga tersedia di `preview/assets/wild-grass-selected.png`, tetapi **belum dapat ditempatkan lewat katalog preview atau Android**. Editor native Android, impor preview lintas format untuk objek ini, perilaku sabit, loot, silo, dan pohon/objek lainnya masih belum selesai. Build dan uji perangkat Android belum dilakukan. Efek rumput dekoratif otomatis pada tile tidak diubah.
