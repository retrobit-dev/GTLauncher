# Pagar kayu dan batu

Konsep kayu dan batu yang disetujui disimpan di `preview/assets/character-redraw/`;
asset transparan dan warna yang diselaraskan untuk preview dan Android dibuat oleh `tools/build_approved_fences.py`. Asset sebelum penyesuaian tetap disimpan.
Aset dan varian pagar lama Android tidak diganti.

Di editor preview, pilih **Bangunan → Pagar kayu/batu**, lalu ketuk tile kering.
Segmen mulai di sisi selatan; putar untuk memilih sisi lain. Jenis dan sisi tetap
aktif untuk penempatan berulang sampai memilih alat lain atau menekan Batal. Setiap segmen
bisa dipilih, dipindah, dihapus, dan di-undo/redo sendiri. Pagar di world dirender sebagai bentuk isometrik dengan sisi, ketebalan dan permukaan atas; pagar batu dibuat lebih rendah dengan blok berselang-seling. Tiang dan ujung bertemu pada
sisi tile sehingga pagar yang bersebelahan dapat membentuk garis atau sudut. Ikon katalog dibuat dari bentuk yang sama (`tools/build_fence_mesh_icons.py`); ilustrasi sebelumnya tetap disimpan.
Pagar menghalangi gerakan melintasi sisinya, bukan seluruh tile. Dua representasi
sisi yang sama dari tile berlawanan ditolak sebagai duplikat.

Data `fences` terpisah dari tanaman dan disimpan dalam format world preview v13.
Impor format lebih lama tidak boleh menimpa world yang memiliki pagar. Android
mendukung v13 untuk subset world **datar** yang sudah didukung (tanpa blockStacks
atau pohon jeruk preview); file yang tidak didukung ditolak, bukan diimpor sebagian.
Di Android, pilih **World → Katalog objek → Bangunan**, jenis pagar dan sisi, lalu
ketuk beberapa tile; indikator di atas layar membatalkan penempatan. Pagar preview dapat dihapus melalui tangan di depan player. Memindahkan
atau memutar segmen yang sudah dipasang di Android belum tersedia; hapus dan pasang
kembali. World Android lama tetap menggunakan varian pagar lamanya.

Penempatan di dalam tapak rumah, di lubang/air dan pada sisi yang sudah berpagar
ditolak. Pada preview, segmen di block bertingkat mengikuti ketinggian tile miliknya;
pada tile beda ketinggian ujungnya tidak dipaksa tersambung. Klip bayangan kontak
mengikuti permukaan block; bayangan proyeksi lembut juga bergerak mengikuti arah cahaya
dan dibatasi agar tidak melebihi dimensi visual pagar. Full Android build/perangkat belum diverifikasi di
lingkungan tanpa Android SDK.

Penempatan berulang juga berlaku untuk tanaman, rumput dan pohon yang tersedia.
Rumah player tetap satu per world, sehingga mode rumah berakhir setelah penempatan.
Di Android, pilihan objek yang tersedia tetap terbatas pada katalog Android dan world
datar yang didukung; memilih alat, membuka World, tombol Batal, atau Back menghentikan
penempatan. Setiap semak panen baru tetap mendapat identitas tersendiri.

Tampilan isometrik ini merupakan proyeksi 2.5D, bukan model 3D atau fisika 3D.
Data world dan format penyimpanan tidak berubah.

## Tembok batu tinggi (varian dihentikan)

Tembok batu tinggi telah dihapus dari katalog penempatan dan pilihan pengganti di preview dan Android. World lama yang sudah memilikinya tetap dapat dibuka, dirender, diekspor dan diedit; objeknya dapat dihapus melalui daftar. Data tidak dihapus otomatis. Format preview v17 dan guard downgrade tetap didukung supaya world lama tidak hilang. Android belum diuji lewat build/perangkat.

## Pilihan banyak objek

Daftar objek preview dan Android menyediakan kotak pilihan untuk penghapusan atau penggantian banyak objek sekaligus. Satu jenis pengganti diterapkan pada posisi masing-masing; semua tapak diperiksa bersama dan seluruh operasi dibatalkan jika tidak aman atau data telah berubah. Identitas dan siklus tanaman baru dimulai ulang. Karakter dan rumah tetap dilindungi. Preview mendukung Undo/Redo; Android subset datar belum menyediakan Undo/Redo dan belum diuji di perangkat.
