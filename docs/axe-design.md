# Kapak: rencana fungsi

Kapak digunakan untuk menebang pohon dan tunggul kayu. Hasil yang diminta adalah Wood (kayu bangunan), Hardwood (dari tunggul besar atau pohon mahoni), Resin, Sap, dan Tree Seeds. Jumlah, peluang, variasi menurut jenis pohon, regrowth, serta aturan penyimpanan saat tas penuh belum ditetapkan; jangan mengasumsikan setiap objek pasti menghasilkan semua item.

## Keadaan saat ini

Ikon kapak telah diterapkan pada preview dan Android; fungsi tebang belum aktif. Android mempunyai pohon buah, tetapi belum ada objek tunggul atau spesies mahoni; preview belum mempunyai objek pohon dan inventaris yang setara. State world dan inventaris Android tersimpan terpisah: penghapusan objek dan pemberian beberapa hasil perlu transaksi aman atau pemulihan yang diuji, agar tidak terjadi kehilangan atau duplikasi saat gagal menyimpan. Pengujian perlu mencakup world lama dan data inventaris yang sudah ada, serta penanganan tas penuh. Jangan hapus atau ubah world pengguna sebagai jalan pintas.
