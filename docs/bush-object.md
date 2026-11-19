# Semak dan panen

Semak polos, melati putih dan murbei masing-masing tersedia ukuran 1×1 dan 2×2 di **Objek → Tanaman dan pohon** pada preview, dan **World → Katalog objek** pada Android untuk world preview datar yang didukung. Melati dan murbei adalah interpretasi spesies tropis dari variasi `flower` dan `berry` yang sudah ada; artwork variasi lama tetap disimpan. Lihat `docs/tanaman-dataran-rendah.md` untuk batasan ekologis dan sumber.

## Siklus gameplay dan panen

Waktu dipercepat yang dipilih adalah **5 menit per siklus**: selama 3 menit semak polos, lalu 2 menit bunga melati atau buah murbei terlihat dan dapat dipanen. Setelah dipanen, tanaman kembali polos sampai siklus berikutnya. Ini siklus permainan, bukan prediksi waktu berbunga aktual atau kalender musim Rembang. Melati dan murbei tidak dibatasi hanya satu musim; simulasi kebutuhan air dan dampak musim belum diterapkan. Waktu dihitung dari jam perangkat sehingga tetap berlanjut saat aplikasi tertutup. Jika jam mundur sebelum waktu tanam, tanaman tidak siap sampai waktunya kembali; riwayat panen mencegah panen ulang siklus yang sama.

Preview: pilih alat tangan, hadapkan karakter ke semak yang bersebelahan, lalu ketuk semak atau karakter. Panen masuk ke tampilan Tas preview; status dan jumlah disimpan bersama dalam satu catatan browser terpisah dari data jeruk. Android: alat tangan → pilih semak di depan → **Panen Melati/Murbei**. Barang dan riwayat panen ditulis bersama dalam inventaris Android; data world dan inventaris lain tidak dihapus. Kedua penyimpanan tidak disinkronkan otomatis. Bila penyimpanan tidak valid/penuh atau perubahan gagal, panen ditolak.

Semak baru menyimpan `plantId` dan `plantedAt`, sehingga dipindah/putar/undo tidak mengulang panen. JSON preview yang memuat keduanya memakai **v12**; Android hanya mengimpor subset datar v12 yang didukung. Impor versi lebih lama tidak boleh mengganti world dengan identitas tanaman. Semak variasi lama tanpa identitas tetap terbaca, dan memakai siklus global; sebagai pengaman anti-duplikasi, semak lama dari jenis yang sama dalam satu world berbagi satu panen per siklus. Semak native Android lama dengan varian bunga/beri juga menggunakan aturan warisan tersebut. Data world lama tidak dimigrasi atau dibuang.

Format inventaris Android menerima riwayat dengan waktu UTC untuk siklus dipercepat di samping tanggal panen lama. Versi aplikasi Android lama mungkin tidak dapat membaca riwayat waktu baru dan harus menolak data, bukan menghapusnya. Build dan uji perangkat Android belum dilakukan.
