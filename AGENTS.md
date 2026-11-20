# Peran karakter dan arah desain

Gunakan istilah **player** untuk menyebut karakter pengguna dalam percakapan dan fitur baru.

Karakter menggabungkan peran **farmer, builder, adventurer, peternak, dan fisherman**. Untuk pekerjaan berikutnya, sesuaikan desain, ikon, animasi, dan fitur dengan gabungan peran ini serta gaya game yang sudah disetujui; ikuti arahan spesifik pengguna. Jangan menganggap setiap fitur harus mencakup ketiga peran sekaligus.

# Aturan pembuatan ikon dock

Gunakan gaya pixel art yang selaras dengan karakter dan konsep glove terpilih (`preview/assets/character-redraw/glove-pixel-concept.png`): siluet jelas pada ukuran dock, bentuk dan warna bersahaja, detail yang terbaca tanpa berlebihan, serta sesuai peran farmer, builder, adventurer, peternak dan fisherman. Buat orientasi alat miring/diagonal seperti ikon cangkul dan sekop yang disetujui, termasuk desain alat berikutnya; pertahankan keterbacaan dan bentuk alami tiap alat. Jadikan referensi visual pengguna sebagai acuan saat relevan. Tawarkan konsep terlebih dahulu bila diminta; jangan mengganti ikon lain tanpa permintaan. Untuk ikon yang disetujui, siapkan latar transparan dan ukuran dock yang sesuai, terapkan secara konsisten di preview dan Android tanpa mengubah fungsinya, lalu uji keterbacaan, tata letak dan interaksi. Simpan sumber dan cadangan sebelum mengganti aset; jelaskan jika belum diuji pada perangkat fisik.

# Aturan pembuatan animasi

Aturan ini berlaku untuk animasi baru dan revisi animasi di aplikasi ini. Ikuti instruksi pengguna yang lebih spesifik jika ada.

## Alur kerja

1. Jadikan desain karakter dan aset yang sedang disetujui sebagai acuan. Pertahankan identitas, pakaian, topi, tas, proporsi, arah hadap, ukuran dalam dunia dan posisi kontak dengan tanah, kecuali diminta mengubahnya. Simpan checkpoint sebelum mengganti aset yang digunakan.
2. Susun pose kunci yang konsisten untuk setiap arah yang diperlukan. Untuk gerakan siklik seperti jalan atau lari, buat kedua langkah yang benar-benar bergantian: kaki dan lengan berlawanan bergerak selaras, dengan sambungan anatomi jelas. Jangan menyebut salinan pose yang sama sebagai langkah lawan. Pencerminan seluruh sprite hanya untuk arah hadap yang sesuai, bukan sebagai pengganti langkah lawan pada arah yang sama.
3. Gambar pose secara konsisten atau gunakan rig dengan aset terpisah per anggota tubuh, termasuk overlap dan pengisian bagian yang tertutup. Tetapkan titik sendi, urutan lapisan dan pivot; periksa bahu, siku, pinggul, lutut, tangan serta sepatu agar tidak terpotong, terlepas, bertambah atau berubah bentuk secara tidak wajar. Jangan memakai potongan persegi yang meninggalkan sambungan terlihat.
4. Buat pose antara dan siklus yang menyambung dengan halus. Sesuaikan timing dan karakter gerak dengan aksi: jalan dan lari harus terlihat berbeda; pada lari, perhatikan perpindahan berat, lutut, ayunan lengan dan fase melayang. Jaga ukuran, posisi tanah dan arah gerak. Tas atau aksesori boleh bereaksi terhadap gerak tanpa terlepas dari karakter.
5. Tinjau frame individual **dan** animasi berulang untuk kedua langkah dan seluruh arah yang didukung. Periksa siluet, sambungan, konsistensi desain, transisi saat berubah arah, awal/akhir gerakan dan tampilan pada ukuran gameplay. Referensi konsep, panduan sendi, deformasi gambar dan pose yang belum konsisten bukan frame final.
6. Setelah lolos pemeriksaan, terapkan secara konsisten pada preview dan Android yang mendukung fitur tersebut; pertahankan hit testing, posisi dunia, kamera, interaksi, opsi reduced motion dan perilaku data. Pastikan aset yang bersesuaian sama dan hindari cache sprite lama.
7. Jalankan tes terkait, termasuk urutan frame, pergantian langkah, arah, kesesuaian aset antar-platform dan build/unit test Android; lakukan tes visual browser dan perangkat jika tersedia. Jelaskan batas pengujian dan bagian yang belum selesai. Jangan menyatakan rig, rotoscoping atau gerakan realistis sudah selesai jika belum benar-benar ada atau belum terverifikasi.

## Kondisi saat ini

`tools/character_rig/run_rig.py` dan `preview/assets/character-redraw/run-rig/joints.json` hanyalah panduan sendi awal. Aset anggota tubuh terpisah, sambungan/occlusion yang rapi, pose yang digambar konsisten dan integrasi runtime belum tersedia. Studi di `preview/assets/character-redraw/run-keyposes/` bukan rangkaian animasi final. Jangan mengganti animasi yang ada dengan panduan tersebut sebelum tahapan di atas selesai.
