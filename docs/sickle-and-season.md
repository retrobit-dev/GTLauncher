# Rencana sabit, silo, dan musim

Keputusan pengguna untuk implementasi berikutnya:

- Sabit menargetkan rumput liar dan semak liar; hasil per objek: Hay 100%, Fiber 60%, Wild Seeds 30%, peluang independen.
- Silo adalah bangunan. Hay masuk ke silo; jika tidak ada silo atau penuh, Hay masuk ke tas. Fiber dan Wild Seeds masuk ke inventaris.
- Wild Seeds mengikuti musim Indonesia. Informasi musim harus disediakan terlebih dahulu; jika hujan belum terdeteksi atau data tidak tersedia, gunakan kemarau sebagai default, ditandai sebagai default.
- Jenis bibit untuk musim hujan/kemarau, kapasitas silo dan perilaku saat tas penuh belum ditetapkan.

## Keadaan implementasi

Ini belum mengaktifkan sabit atau silo. Preview dan Android kini memiliki objek rumput/semak serta panen melati dan murbei; tas preview masih terbatas pada hasil yang didukung, tidak setara inventaris Android. Sabit dan silo belum tersedia; data world dan inventaris berada di penyimpanan terpisah. Menghapus objek sekaligus memberikan hasil perlu transaksi aman atau pemulihan agar kegagalan penyimpanan tidak menghilangkan atau menggandakan barang. Jangan menampilkan fungsi tersebut sebagai tersedia sampai keduanya bekerja dan diuji.

HUD sudah menghitung **perkiraan nonresmi** musim hujan/kemarau berdasarkan curah hujan historis tiga dasarian, dengan lokasi perangkat atau lokasi cadangan. Jika jaringan/data tidak tersedia, gameplay memakai `dry` sebagai default, bukan pengamatan; status estimasi tetap `unknown`. Preview menyediakan `window.previewSeason()` dan event `previewseasonchange`, dengan `window.previewSeasonInfo()` untuk membedakan default dari estimasi; Android menyimpan nilai perkiraan di `gameplaySeason` dan status default di `seasonIsDefault`. Keduanya belum digunakan untuk menghasilkan bibit. Perkiraan lokasi cadangan tidak selalu mewakili lokasi pengguna.
