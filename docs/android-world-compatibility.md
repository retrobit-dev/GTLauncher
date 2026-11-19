# Kompatibilitas world Android

Penyimpanan browser dan Android terpisah; tidak ada sinkronisasi otomatis. Android dapat mengimpor JSON `gtlauncher-preview-world` datar versi 1–3 yang seluruh field-nya didukung dan lulus validasi. Impor menggunakan pemilih berkas Android dan meminta konfirmasi bila ID sudah ada. Android tidak menyediakan ekspor tambahan atau mengubah inventaris selama impor.

World preview versi 4–8 (terrain bertingkat, pohon dan cemara) belum didukung Android. Impor ditolak sebelum penyimpanan, dengan pemberitahuan versi belum didukung; field yang tidak dikenal juga ditolak. Jangan mengubah nomor versi untuk memaksa impor: data tidak dapat ditampilkan atau dimainkan secara benar.

Format `gtlauncher-world` lama dan `gtlauncher-preview-world` adalah skema berbeda. Impor salah satu tidak dapat menggantikan world dengan ID yang sama dalam skema lainnya, karena terrain atau objek dan portal dapat hilang. Impor preview versi lama juga tidak boleh menggantikan terrain yang memuat jenis tile yang versi tersebut tidak dapat menyimpan. Penolakan tidak mengubah world aktif atau data tersimpan. Impor dengan ID baru tetap tersedia; pergantian dalam skema yang sama tetap meminta konfirmasi dan dapat mengganti isi sesuai berkas.

Pengujian mencakup penolakan v4–v8 dan penggantian lintas skema, pelestarian data dan ID aktif, serta perubahan terrain yang tidak dapat diekspresikan versi lama. Belum ada build atau uji perangkat Android; dukungan penuh v4–v8, katalog/editor, cemara dan sinkronisasi masih perlu dikerjakan.

## Fondasi terrain bertingkat

Android sekarang memiliki model `BlockTerrain` immutable yang memvalidasi daftar block per sel (maksimum 32, nilai 1–6 dan permukaan sesuai `tiles`), menyediakan ketinggian dan aturan langkah naik/turun satu tingkat, serta memperlakukan world lama tanpa stack sebagai terrain datar. Rute Android untuk world preview datar menggunakan aturan yang sama; uji Java mencakup tanjakan, tebing, jalan memutar, data tidak valid dan perubahan input setelah pembacaan.

Ini **belum** mengaktifkan impor world bertingkat: parser world Android masih menolak v4–v8. Renderer, posisi player dan objek berdasarkan tinggi, penyimpanan/editing, validasi objek dan pohon, serta interaksi perlu diselesaikan dan diuji sebelum format dibuka. Model tersebut belum tersambung ke data v4–v8 dalam aplikasi yang berjalan.
