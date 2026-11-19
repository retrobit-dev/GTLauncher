# Kompatibilitas world Android

Penyimpanan browser dan Android terpisah; tidak ada sinkronisasi otomatis. Android dapat mengimpor JSON `gtlauncher-preview-world` datar versi 1–3 yang seluruh field-nya didukung dan lulus validasi. Impor menggunakan pemilih berkas Android dan meminta konfirmasi bila ID sudah ada. Android tidak menyediakan ekspor tambahan atau mengubah inventaris selama impor.

World preview bertingkat versi 4–12 dan world berpohon jeruk belum didukung Android. Pengecualian: JSON v7/v8 world datar yang berisi cemara 1×1 atau 2×2, v9 world datar yang berisi cemara dan/atau rumput liar, v10 world datar yang juga dapat berisi semak, v11 world datar dengan variasi semak dan tapak 2×2, serta v12 world datar dengan identitas dan waktu tanam semak dapat diimpor; cemara memakai gambar, goyangan dan tabrakan di Android. Berkas yang belum didukung ditolak sebelum penyimpanan; field yang tidak dikenal juga ditolak. Jangan mengubah nomor versi untuk memaksa impor: data tidak dapat ditampilkan atau dimainkan secara benar.

Format `gtlauncher-world` lama dan `gtlauncher-preview-world` adalah skema berbeda. Impor salah satu tidak dapat menggantikan world dengan ID yang sama dalam skema lainnya, karena terrain atau objek dan portal dapat hilang. Impor preview versi lama juga tidak boleh menggantikan terrain yang memuat jenis tile yang versi tersebut tidak dapat menyimpan. Penolakan tidak mengubah world aktif atau data tersimpan. Impor dengan ID baru tetap tersedia; pergantian dalam skema yang sama tetap meminta konfirmasi dan dapat mengganti isi sesuai berkas.

Pengujian mencakup impor terbatas cemara datar dan penolakan data v4–v12 yang belum didukung dan penggantian lintas skema, pelestarian data dan ID aktif, serta perubahan terrain yang tidak dapat diekspresikan versi lama. Belum ada build atau uji perangkat Android; dukungan penuh v4–v12, katalog/editor, cemara dan sinkronisasi masih perlu dikerjakan.

## Fondasi terrain bertingkat

Android sekarang memiliki model `BlockTerrain` immutable yang memvalidasi daftar block per sel (maksimum 32, nilai 1–6 dan permukaan sesuai `tiles`), menyediakan ketinggian dan aturan langkah naik/turun satu tingkat, serta memperlakukan world lama tanpa stack sebagai terrain datar. Rute Android untuk world preview datar menggunakan aturan yang sama; uji Java mencakup tanjakan, tebing, jalan memutar, data tidak valid dan perubahan input setelah pembacaan.

Ini **belum** mengaktifkan impor world bertingkat: parser world Android masih menolak stack v4–v12. Renderer, posisi player dan objek berdasarkan tinggi, penyimpanan/editing, validasi objek dan pohon, serta interaksi perlu diselesaikan dan diuji sebelum format dibuka. Model tersebut belum tersambung ke data v4–v12 dalam aplikasi yang berjalan.


### Block beratap rumput (preview v14)
Preview menyediakan block tanah, batu, pasir, dan tanah garapan dengan permukaan atas rumput; sisi tetap memakai bahan asal. Air tidak memiliki varian ini. ID 7–10 tersimpan sebagai jenis block terpisah, termasuk bila berada di bawah block lain, sehingga ekspor yang memuatnya menggunakan format preview v14. Format lama tidak dapat mengganti world yang mengandung ID tersebut.

Android menerima dan menggambar v14 **datar** pada versi aplikasi yang diperbarui, dengan batasan impor objek yang sama seperti sebelumnya. Block bertingkat belum didukung Android dan ditolak tanpa mengganti data. Pemilih block dan editor terrain Android belum tersedia; aplikasi Android lama tidak mendukung v14.
