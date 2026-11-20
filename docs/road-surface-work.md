# Jalan dan sungai

Objek jalan 1×1 di katalog khusus **Jalan**: tanah, batu, pasir, paving, aspal,
dan sungai. Warna dan detail tiap material mengikuti konsep terpilih
`artwork/road-and-river-concept.png`; sungai mendapat animasi sorotan aliran searah objek. Jalan
sejenis dan setinggi sama di preview menyambung ke tepi tile.

Di Map Editor, pilih world buatan, buka **Objek**, pilih jenis jalan, lalu ketuk
tile untuk menempatkannya; ketuk kembali jalan di tile tersebut untuk menghapusnya.
Penempatan dapat diulang; gunakan Batal untuk berhenti. Undo/Redo tersedia.
Daftar objek juga menyediakan Hapus dan Ganti dengan jenis jalan lain. Jalan
belum mendukung pemilihan langsung pada kanvas, seleksi area, opacity manual,
atau penggantian lintas kategori objek. Jalan tidak ditempatkan di lubang,
terrain air, ramp, rumah, atau tanaman; sungai tidak boleh ditempatkan di
player kecuali ada jembatan. Tile dan block di bawah jalan tidak diubah.

Sungai adalah rintangan routing, bukan block air dan bukan sumber air untuk
alat. Jalan lain dapat dilewati sesuai aturan terrain, ketinggian, pagar dan
objek yang ada. Jembatan yang sudah valid dapat dilalui di atas sungai.
Jalan yang sudah ditempatkan tetap mengikuti permukaan block teratas. Perubahan
terrain/objek yang menyebabkan tapak jalan menjadi tidak valid ditolak.

Format preview **v30** menyimpan `roads` secara terpisah; berkas versi lama tidak
boleh menggantikan world yang memiliki jalan. Android membaca v30, menampilkan
jalan sebagai objek permukaan, dan memperlakukan sungai sebagai air untuk
routing. Browser dan Android tetap memakai penyimpanan terpisah. Pemeriksaan
Java dan browser bukan pengujian APK/perangkat; tampilan Android belum diuji
pada perangkat, dan tampilan permukaan Android tidak identik piksel-per-piksel
dengan preview.

Arah aliran sungai dapat dipilih dengan Putar kiri/kanan sebelum penempatan;
untuk sungai yang sudah ada gunakan Putar pada daftar objek. Arah tersimpan
dan mengikuti rotasi kamera tanpa mengubah orientasi world. Sungai lama tanpa
arah memakai Selatan secara default. Animasi mengikuti pengaturan reduced motion.
Sambungan hanya antara tile sejenis dengan ketinggian sama. Format preview v31
diperlukan untuk menyimpan arah; impor versi lebih lama tidak menggantikan world
yang memiliki arah sungai. Android belum diuji pada perangkat.
