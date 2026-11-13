# Base World

Base World adalah dasar saat ini untuk pengembangan world dengan pulau-pulau yang berbeda fungsinya. Dokumen ini mencatat implementasi sekarang. Preview browser menyediakan pengelola world berbasis Base World; aplikasi Android dapat mengimpor dan memilih world yang diekspor. Ini belum menjadi sistem konfigurasi pulau generik.

## Pulau

- Pulau terapung memiliki kisi 9×9, kolom A–I dan baris 1–9, dengan proyeksi dimetrik. Timur mengarah ke kanan-bawah layar. Laut dan karakter tidak ditampilkan di Base World.
- Bayangan pulau menggunakan aturan tersendiri, bukan proyeksi bayangan awan. Posisinya tetap dan terpusat di bawah pulau; ukuran dan bentuknya tidak berubah mengikuti waktu atau cuaca. Opasitasnya disesuaikan terhadap latar agar tetap terlihat.

## Langit referensi

Kedua kisi mengikuti bentuk dan ukuran pulau. Keduanya **disembunyikan**, bukan dihapus; koordinatnya tetap digunakan.

| Kisi | Tinggi dari pulau | Geser ke barat | Warna garis jika ditampilkan |
|---|---:|---:|---|
| Langit Matahari | 100 unit scene | 20 unit | `#0b1420` |
| Langit Awan | 50 unit scene | 10 unit | `#ffffff` |

Pergeseran barat diukur sepanjang sumbu kisi yang terproyeksi. Jika ditampilkan, Langit Matahari digambar di atas Langit Awan pada bagian yang bertumpuk. Pada ketinggian ini sebagian Langit Matahari berada di luar layar.

## Matahari dan Bulan

- Keduanya menggunakan busur yang sama pada Langit Matahari dan mencapai pusat E5 saat transitnya masing-masing. Ini berlaku untuk live dan seluruh demo.
- Matahari tersembunyi di bawah horizon. Bulan disembunyikan pada siang hari; ketika muncul, bentuk fasenya tetap mengikuti data astronomi.
- Sampai ketinggian Matahari −2°, Matahari menjadi sumber proyeksi bayangan awan; setelah itu Bulan digunakan. Jika Bulan berada di bawah horizon, posisi Bulan virtual masih digunakan agar bayangan malam tetap ada. Ini adalah konvensi visual, bukan simulasi cahaya Bulan fisik.
- Pada posisi dan ketinggian kisi sekarang, sebagian besar cakram Matahari saat berada di E5 terpotong oleh batas atas layar.

## Awan dan bayangannya

- Awan bergerak pada Langit Awan sepanjang sumbu kisi kardinal terdekat dengan arah angin bertiup. Ada toleransi 5° melewati batas pergantian sumbu; angin di bawah 3 km/jam menghentikan gerak. Kecepatan menggunakan gain 0,05 dan batas 4,2 unit scene per detik.
- Jumlah awan mengikuti tutupan awan: minimal satu, lalu dua pada 32%, tiga pada 58%, dan empat pada 82%. Opasitas awan 0,55. Lebar dan tinggi sprite awan serta bayangannya adalah ⅔ dari ukuran sebelum pengecilan terakhir.
- Kedalaman gambar awan dibandingkan dengan sumber cahaya aktif: pusat awan yang lebih tinggi di layar digambar di belakangnya, yang lebih rendah di depan.
- Pusat bayangan awan adalah titik potong garis dari sumber cahaya melalui pusat awan ke bidang pulau atau perpanjangan visualnya di luar pulau. Misalnya saat Matahari di E5, pusat awan F5 memproyeksikan ke G5; H5 memproyeksikan ke K5 di luar kisi pulau.
- Bentuk bayangan mengikuti sprite awan dengan skala 0,78 terhadap awan, sama di semua cuaca. Bayangan di luar pulau tetap digambar; bidang luarnya merupakan perpanjangan visual bidang pulau, bukan lantai yang dimodelkan secara fisik terpisah. Opasitas bayangan mengikuti cahaya dan cuaca.

## World di preview browser

- Tombol **World** di sudut kiri atas di atas tablet membuka daftar world. Base World tetap tersedia dan tidak dapat diedit atau dihapus lewat pengelola preview. Home bawaan telah dihapus; Home buatan pengguna tetap tersimpan dan dapat diedit.
- Karakter utama dapat ditampilkan pada world buatan. Posisi default E5, dan kakinya selalu berpusat pada tile A1–I9 yang dipilih. Tombol **Edit** di samping Ganti suasana mengaktifkan mode editing: ketuk karakter untuk highlight, drag untuk pindah ke pusat tile terdekat, dan pinch untuk skala 50–150%. Tombol putar di kanan tablet mengubah arah hadap Utara/Timur/Selatan/Barat dan termasuk dalam Undo/Redo. Tombol **Pilih karakter** dan **Pengaturan objek** menyediakan alternatif tanpa gestur sentuh, termasuk pilihan kolom/baris dan skala. Perubahan disimpan pada akhir gestur dan hanya pada world aktif. Tombol ikon Undo dan Redo di kanan atas tablet aktif dalam mode editing, mencatat hingga 50 perubahan karakter per world di tab yang sama; riwayat tidak bertahan setelah halaman dimuat ulang. Karakter memiliki idle berupa perubahan tinggi satu piksel render sambil mempertahankan posisi kaki; mode reduced-motion menghentikannya. Home lama tanpa pengaturan karakter ditampilkan secara default. Karakter belum berjalan atau berpindah melalui portal.
- World baru mewarisi aturan Base World. Pengguna dapat mengatur nama, catatan fungsi, dan warna pulau; membuat world langsung membukanya. World tersimpan dapat dibuka, diedit, diurutkan dengan tombol naik/turun, diekspor, atau dihapus dengan konfirmasi; Base World tetap paling atas. Menghapus world aktif kembali ke Base World.
- World buatan dan pilihan aktif di preview disimpan di `localStorage` browser/perangkat tersebut. Data tidak otomatis tersinkron ke Android dan dapat hilang jika data browser dihapus. Catatan fungsi hanya metadata; belum mengubah perilaku pulau. Warna hanya mengubah tint pulau, tidak mengubah geometri atau aturan cuaca.
- Jika Home bawaan lama sedang aktif, pemilihan kembali ke Base World; data world buatan termasuk Home tidak diubah. Pengguna dapat membuka Home buatannya dari daftar.

## Format bersama dan ekspor ke Android

- Karakter dan rumah memiliki bayangan permukaan yang menggunakan proyeksi dan sumber cahaya yang sama: Matahari di siang hari dan Bulan (termasuk posisi virtual di bawah horizon) di malam hari. Bayangannya adalah perkiraan berbentuk tapak yang diekstrusi, bukan siluet detail sprite; geometri mengikuti posisi dan skala objek dan dibatasi agar tidak melebihi setengah lebar maupun tinggi sprite; opasitas memiliki batas minimum agar terlihat pada malam dan cuaca mendung. Bayangan objek dibatasi ke permukaan pulau; aturan bayangan awan di luar pulau dan bayangan pulau yang tetap tidak berubah. Objek masa depan harus didaftarkan ke fungsi bayangan bersama saat tipe objeknya dibuat; sistem objek umum belum tersedia.
- Rumah memiliki tapak logis 3×3 dan awalnya berada di A1–C3 pada world bernama Home; ada ruang di tepinya pada skala 100%. Home lama tanpa data rumah ditambahkan sekali saat preview dimuat, dan Home baru mendapatkannya saat dibuat. Dalam mode Edit, ketuk rumah atau gunakan Pilih rumah, lalu drag, pinch, putar, atau gunakan Pengaturan objek; perubahan termasuk visibilitas mendukung Undo/Redo per world. Sudut pertama rumah dibatasi A1–G7 agar tapak 3×3 tetap di pulau; gambar yang diperbesar di atas 100% dapat melampaui tapak. World lain dan Base tidak berubah.
- `world/world-v6.schema.json` mendefinisikan ekspor maksimal satu karakter dan satu rumah yang dapat diubah posisi, skala, arah dan visibilitasnya. World tanpa rumah tetap menggunakan versi 4. Jenis objek dan field yang belum didukung ditolak Android agar tidak hilang diam-diam; schema versi 2–5 dipertahankan.
- Pada preview, tekan **Ekspor** pada world buatan. Pindahkan berkas ke Android. Dari homescreen, geser ke atas untuk membuka daftar aplikasi, pilih **World → Impor world**, lalu pilih berkas JSON. World valid disimpan dan dibuka.
- Ekspor ulang world yang sama mempertahankan ID; impor versi 2, 3, 4, 5 atau 6 dengan ID yang sama memperbarui salinan Android, bukan menambahkan duplikat. Impor versi 1 yang lama tetap diterima sebagai world baru karena tidak memiliki ID stabil; ia tidak dapat dicocokkan otomatis dengan ekspor berikutnya. Impor versi 2 ditolak jika akan menghapus karakter yang sudah tersimpan; gunakan ekspor terbaru. Impor versi 3 juga ditolak jika akan menghapus arah non-default yang sudah tersimpan. Versi lebih lama dari 5 ditolak jika akan menghapus rumah tersimpan; versi 5 ditolak jika akan menghapus pengaturan rumah yang telah diedit.
- Preview juga memiliki **Impor JSON**: versi 1–6 divalidasi sebelum disimpan, ID yang sama meminta konfirmasi, dan format lama yang akan menghapus data objek ditolak. Nama yang sama dengan ID berbeda juga ditolak. Berkas maksimal 64 KiB. Versi 1 diimpor sebagai salinan baru.
- Android: pemilih menampilkan semua jenis file karena beberapa penyedia memberi label MIME yang keliru; kontennya tetap divalidasi dan ID yang sama meminta konfirmasi. Berkas maksimal 64 KiB dan divalidasi sebelum menyimpan. Warna diterapkan sebagai tint pulau; karakter dan rumah dirender di Android (belum diuji pada perangkat); catatan tetap metadata. Android belum menyediakan ekspor, edit atau penghapusan world impor, dan tidak ada sinkronisasi otomatis.

## Batasan yang diketahui

- Editor objek mendukung satu karakter dan satu rumah. Katalog bersama dan penambahan beberapa salinan objek, pagar di world, aturan bersama, dan portal belum tersedia.
- Pusat awan berputar kembali saat mencapai batas kisi, tetapi sprite belum dibagi menjadi salinan yang menyambung mulus pada batas tersebut.
- Model yang sesuai ada di preview dan sumber Android. Preview telah diuji dengan angka piksel dan `node tools/test_base_world.js`; sumber Android telah diperiksa sintaksnya, tetapi belum dikompilasi atau dijalankan di lingkungan ini karena toolchain Android tidak tersedia.
- Referensi teknis dan detail rendering tambahan ada di `preview/README.md`.

### Aset pagar (belum terintegrasi)

`preview/assets/fence-segment.png` adalah satu segmen pagar kayu transparan untuk satu garis batas tile, bukan pagar yang mengelilingi area. Pada resolusi render 2×, titik kaki tiang pada aset adalah (2, 29) dan (30, 17), sehingga berjarak 28×−12 piksel backing (mendekati satu sisi grid 14×−5,9167 satuan scene). Cerminkan secara horizontal untuk sisi grid miring lainnya dan sejajarkan kaki tiang dengan kedua ujung garis tile; gunakan koordinat geometris grid, bukan posisi piksel yang dibulatkan, saat menyambungkan segmen. Pintu pagar belum dibuat. Aset ini belum tersedia dalam katalog atau ekspor, belum ditempatkan pada world, dan belum dapat diedit.
