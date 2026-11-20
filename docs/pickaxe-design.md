# Beliung: kebutuhan fungsi

Beliung menghancurkan batu di kebun dan gua tambang untuk menghasilkan Stone, Coal, Ore (Bronze, Silver, Gold, Osmium), Geodes (berisi permata/artefak), dan Fossils. Jumlah, peluang, jenis batu, pembukaan geode, dan perilaku saat tas penuh belum ditentukan; jangan mengasumsikan semua hasil muncul dari setiap batu.

## Keadaan saat ini

Ikon beliung diterapkan pada preview dan Android; mekanik menghancurkan batu dan mendapatkan hasil belum aktif. Gua tambang, objek batu tambang yang bisa dihancurkan, pembukaan geode, dan inventaris preview belum tersedia. Material tile batu bukan otomatis batu tambang dan tidak boleh dihancurkan sebagai jalan pintas. State world dan inventaris Android disimpan terpisah: pemberian hasil serta penghapusan objek membutuhkan transaksi atau pemulihan yang aman dan diuji agar tidak kehilangan atau menggandakan barang. Pertahankan world dan inventaris lama.
