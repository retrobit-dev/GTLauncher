# Pohon jeruk

Konsep terpilih `preview/assets/character-redraw/orange-tree-concept.png` digunakan untuk sprite pohon dewasa berbuah. Dalam Map Editor preview, **Objek → Pohon jeruk → Tempatkan pohon jeruk**, lalu pilih sudut pertama tapak 2×2. Dapat ditempatkan lebih dari satu pada tanah rata tanpa air, lubang, rumah, pohon lain, atau player. Pohon menjadi rintangan jalur, bergoyang mengikuti angin (diam pada reduced motion), memiliki bayangan, tersimpan per world dan mendukung undo/redo penempatan dalam sesi editor. Perubahan block yang merusak tapak ditolak.

Preview saat ini hanya menyediakan penempatan; memilih, memindahkan, menebang, memanen atau pertumbuhan pohon di preview belum diterapkan. Undo dapat membatalkan penempatan selama riwayat sesi masih tersedia. Ekspor dengan pohon memakai JSON preview versi 5, hanya untuk preview; Android belum mendukung format ini. Data preview dan Android tetap terpisah.

Di Android sprite pohon jeruk berbuah matang pada sistem pohon yang sudah ada diperbarui; tahap tidak berbuah masih memakai sprite lama. Katalog editor Android tidak dibuat. Perubahan Android belum diuji lewat build atau perangkat.
