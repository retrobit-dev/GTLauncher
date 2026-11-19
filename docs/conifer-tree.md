# Pohon cemara

Pohon cemara pixel art dibuat berdasarkan referensi siluet bertingkat. Di Map Editor preview, pilih **Objek → Pohon cemara → Tempatkan pohon cemara**, lalu ketuk sudut pertama tapak 2×2. Tinggi tampilan dan titik pijak mengikuti sistem pohon jeruk; sprite mempertahankan proporsi asli. Area harus rata, kering, tanpa lubang, player, rumah, atau pohon lain. Pohon menghalangi rute, bergoyang mengikuti angin dan menghasilkan bayangan dari siluetnya; reduced motion menghentikan goyangan. Pohon dapat dipilih lewat siluet, dipindahkan, diputar, dihapus, serta mendukung Undo/Redo. Rotasi saat ini memakai pencerminan gambar, bukan empat gambar berbeda.

World lama tanpa jenis pohon tetap ditafsirkan sebagai pohon jeruk. Untuk menjaga fitur editor dan validasi tapak yang sudah ada, daftar internal `orangeTrees` juga menyimpan cemara dengan `kind: "conifer"`; pohon jeruk tetap tidak memiliki `kind`. Cemara tidak menghasilkan jeruk. JSON preview yang mengandung cemara memakai versi 7; format lebih lama tidak boleh menghapus jenis pohon ini saat mengganti world. Versi 5/6 tanpa cemara tetap dipertahankan. Format ini hanya didukung preview, bukan Android, dan data Android terpisah.

Penebangan, hasil kayu/resin, pertumbuhan, serta katalog atau renderer cemara Android belum diterapkan. Gambar konsep disimpan di `preview/assets/character-redraw/conifer-pixel-concept.png`; `tools/build_conifer_tree.py` menyiapkan sprite transparan tanpa meregangkan proporsi. Tes browser memeriksa penempatan, batas tabrakan, larangan hasil jeruk, seleksi, rotasi, Undo/Redo, penyimpanan dan pertukaran JSON v7.

## Cemara 1×1

Pilihan **Pohon cemara · Tapak 1×1** ditambahkan terpisah di katalog preview; cemara 2×2 yang sudah ada tidak diubah. Sprite yang sama ditampilkan pada setengah skala dengan akar di tengah tile, dan bayangan, goyangan, seleksi siluet, rute serta validasi tapak mengikuti ukuran itu. Cemara tetap bukan pohon buah. Penempatan, rotasi dan Undo/Redo memakai editor yang ada.

World yang mengandung cemara 1×1 diekspor sebagai JSON preview v8 dengan `kind: "conifer", size: 1`; world dengan cemara 2×2 saja tetap v7. Impor versi lama tidak dapat menggantikan world dengan ukuran baru. Android belum mendukung cemara atau JSON v8; data browser dan Android tetap terpisah.
