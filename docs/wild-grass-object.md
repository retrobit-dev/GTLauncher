# Rumput liar

Konsep pilihan disimpan pada `preview/assets/character-redraw/wild-grass-new-concept.png`. `tools/build_wild_grass_selected.py` menghasilkan sprite transparan untuk preview dan Android tanpa meregangkan proporsi. Gambar lama Android `wild_grass.png` tetap disimpan.

Di preview, buka Map Editor → Objek → Tanaman dan pohon → **Rumput liar** lalu ketuk tile kering 1×1 yang kosong. Objek dapat dipilih melalui siluet, digeser, diputar, dihapus, dan mendukung Undo/Redo. Rumput menghalangi tile seperti objek semak Android yang sudah ada, bergerak mengikuti angin, dan tidak menghasilkan jeruk. Efek rumput dekoratif otomatis pada tile tidak diubah.

Di Android, untuk world portable datar dengan karakter, buka **World → Katalog objek → Tanaman dan pohon → Rumput liar**, lalu ketuk tile kosong yang kering. Objek dapat dipindahkan melalui alat tangan; mengambil ke tas dan undo native belum tersedia. Base World dan world lama nonportable tidak dapat diedit melalui katalog ini. Ini bukan Map Editor Android lengkap.

Untuk menjaga kompatibilitas dengan data pohon yang ada, objek preview disimpan dalam daftar internal `orangeTrees` sebagai `{kind:"wild-grass",size:1}`. World yang memuat rumput tersebut diekspor sebagai JSON v9; impor dari format lebih lama tidak boleh menghapusnya. Android mendukung world v9 **datar** yang berisi rumput liar dan/atau cemara; stack bertingkat, pohon jeruk preview, dan objek lain yang belum didukung tetap ditolak. Browser dan Android tetap memiliki penyimpanan terpisah.

Fungsi sabit, hasil Hay/Fiber/Wild Seeds dan silo belum tersedia. Android belum dibangun atau diuji pada perangkat; uji JVM menggunakan pengganti SharedPreferences dan uji browser tidak memverifikasi antarmuka Android.
