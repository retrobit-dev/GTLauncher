# Jalan dan sungai

Objek jalan 1×1 di katalog khusus **Jalan**: tanah, batu, pasir, paving, aspal,
dan air/sungai. Warna dan detail tiap material mengikuti konsep terpilih
`artwork/road-and-river-concept.png`; jalan air mendapat sorotan aliran. Jalan
sejenis dan setinggi sama di preview menyambung ke tepi tile.

Di Map Editor, pilih world buatan, buka **Objek**, pilih jenis jalan, lalu ketuk
tile untuk menempatkannya; ketuk kembali jalan di tile tersebut untuk menghapusnya.
Penempatan dapat diulang; gunakan Batal untuk berhenti. Undo/Redo tersedia.
Daftar objek juga menyediakan Hapus dan Ganti dengan jenis jalan lain. Jalan
belum mendukung pemilihan langsung pada kanvas, seleksi area, opacity manual,
atau penggantian lintas kategori objek. Jalan tidak ditempatkan di lubang,
terrain air, ramp, rumah, atau tanaman; jalan air tidak boleh ditempatkan di
player kecuali ada jembatan. Tile dan block di bawah jalan tidak diubah.

Jalan air adalah rintangan routing, bukan block air dan bukan sumber air untuk
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
