# Kompatibilitas world Android

Penyimpanan browser dan Android terpisah; tidak ada sinkronisasi otomatis. Android menyediakan pemilih world dan impor, bukan editor atau katalog penempatan. Aksi gameplay seperti pergerakan dan interaksi tetap tersedia. World dan inventaris yang sudah ada tidak dihapus.

Android dapat membaca world preview **datar** versi 1–3 dan 5–17, termasuk pohon jeruk, cemara, rumput, semak, pagar, jembatan dan block beratap rumput yang sesuai versinya. Nomor versi 4 mensyaratkan block bertingkat. World bertingkat (`blockStacks`) semua versi masih ditolak tanpa menyimpan, bukan diratakan atau menghilangkan data. Tampilan dan interaksi Android tidak dijamin identik dengan preview, dan build/perangkat belum diuji. Jangan mengganti nomor versi untuk memaksa impor.

Format `gtlauncher-world` lama dan `gtlauncher-preview-world` adalah skema berbeda. Impor salah satu tidak dapat menggantikan world dengan ID yang sama dalam skema lainnya, karena terrain atau objek dan portal dapat hilang. Guard downgrade menjaga data yang format lama tidak bisa menyimpan. Impor dengan ID baru tetap tersedia; penggantian dalam skema yang sama meminta konfirmasi.

## Fondasi terrain bertingkat

Android sekarang memiliki model `BlockTerrain` immutable yang memvalidasi daftar block per sel (maksimum 32, nilai 1–6 dan permukaan sesuai `tiles`), menyediakan ketinggian dan aturan langkah naik/turun satu tingkat, serta memperlakukan world lama tanpa stack sebagai terrain datar. Rute Android untuk world preview datar menggunakan aturan yang sama; uji Java mencakup tanjakan, tebing, jalan memutar, data tidak valid dan perubahan input setelah pembacaan.

Ini **belum** mengaktifkan impor world bertingkat: parser world Android masih menolak stack semua versi. Renderer, posisi player dan objek berdasarkan tinggi, penyimpanan/editing, validasi objek dan pohon, serta interaksi perlu diselesaikan dan diuji sebelum format dibuka. Model tersebut belum tersambung ke data stack dalam aplikasi yang berjalan.


### Block beratap rumput (preview v14)
Preview menyediakan block tanah, batu, pasir, dan tanah garapan dengan permukaan atas rumput; sisi tetap memakai bahan asal. Air tidak memiliki varian ini. ID 7–10 tersimpan sebagai jenis block terpisah, termasuk bila berada di bawah block lain, sehingga ekspor yang memuatnya menggunakan format preview v14. Format lama tidak dapat mengganti world yang mengandung ID tersebut.

Android menerima dan menggambar v14 **datar** pada versi aplikasi yang diperbarui, dengan batasan impor objek yang sama seperti sebelumnya. Block bertingkat belum didukung Android dan ditolak tanpa mengganti data. Pemilih block dan editor terrain Android belum tersedia; aplikasi Android lama tidak mendukung v14.
