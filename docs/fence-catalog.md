# Pagar kayu dan batu

Konsep kayu dan batu yang disetujui disimpan di `preview/assets/character-redraw/`;
asset transparan untuk preview dan Android dibuat oleh `tools/build_approved_fences.py`.
Aset dan varian pagar lama Android tidak diganti.

Di editor preview, pilih **Bangunan → Pagar kayu/batu**, lalu ketuk tile kering.
Segmen mulai di sisi selatan; pilih/putar untuk memilih sisi lain. Setiap segmen
bisa dipilih, dipindah, dihapus, dan di-undo/redo sendiri. Tiang bertemu pada
ujung sisi tile sehingga pagar yang bersebelahan dapat membentuk garis atau sudut.
Pagar menghalangi gerakan melintasi sisinya, bukan seluruh tile. Dua representasi
sisi yang sama dari tile berlawanan ditolak sebagai duplikat.

Data `fences` terpisah dari tanaman dan disimpan dalam format world preview v13.
Impor format lebih lama tidak boleh menimpa world yang memiliki pagar. Android
mendukung v13 untuk subset world **datar** yang sudah didukung (tanpa blockStacks
atau pohon jeruk preview); file yang tidak didukung ditolak, bukan diimpor sebagian.
Di Android, pilih **World → Katalog objek → Bangunan**, jenis pagar dan sisi, lalu
ketuk tile. Pagar preview dapat dihapus melalui tangan di depan player. Memindahkan
atau memutar segmen yang sudah dipasang di Android belum tersedia; hapus dan pasang
kembali. World Android lama tetap menggunakan varian pagar lamanya.

Penempatan di dalam tapak rumah, di lubang/air dan pada sisi yang sudah berpagar
ditolak. Pada preview, segmen di block bertingkat mengikuti ketinggian tile miliknya;
pada tile beda ketinggian ujungnya tidak dipaksa tersambung. Klip bayangan kontak
mengikuti permukaan block. Full Android build/perangkat belum diverifikasi di
lingkungan tanpa Android SDK.
