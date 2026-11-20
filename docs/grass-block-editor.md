# Block Rumput di Map Editor

Map Editor hanya tersedia di preview. World baru memakai salinan terrain Base World 50×50: empat block tanah dan satu block rumput per tile, yang dapat diedit secara independen. World lama tidak dimigrasi atau diubah ukurannya. Base World terkunci. Pilih world buatan, buka Map Editor, lalu pilih **Block → Rumput**. Pemilih block menampilkan nama lengkap dan thumbnail; pada layar kecil panel alat dapat digulir.

- **Tambah block** menempatkan satu block di atas block paling atas, atau pada sel kosong di dalam grid. Tidak memperluas batas grid; tinggi maksimum 32 block per sel. Tinggi satu block sama dengan tinggi pita rumput di sisi pulau.
- **Ganti block** (pensil) mengganti block paling atas, bukan menambah tingkat. Pada sel kosong gunakan Tambah.
- **Hapus block** menghapus block paling atas; sel menjadi kosong jika block terakhir dihapus. **Ambil jenis block** memilih jenis paling atas. **Isi area block** mengganti permukaan yang terhubung dengan jenis dan ketinggian yang sama, atau mengisi area kosong yang terhubung.
- Ketuk permukaan atau sisi block yang terlihat untuk memilih kolomnya. Gerakan seret yang melewati sel yang sama beberapa kali mengubahnya sekali; satu seret merupakan satu langkah Undo/Redo.
- Ketinggian di bawah karakter atau rumah dilindungi, dan rumah hanya dapat dipindahkan ke tapak yang rata. Jika penyimpanan gagal atau data berubah di tab lain, perubahan ditolak.

Gameplay preview menggunakan permukaan teratas: karakter dapat naik atau turun satu tingkat per langkah; sel kosong dan perbedaan dua tingkat atau lebih menghalangi rute. Jalur dapat memutar melalui permukaan bertangga. World datar lama tidak perlu migrasi. Riwayat Undo/Redo berlaku selama sesi tab.

World bertingkat memakai format JSON preview versi 4 saat diekspor; versi Android saat ini **belum mendukung** format ini. Data browser dan Android terpisah dan tidak otomatis tersinkron. Efek pencahayaan, bayangan serta urutan gambar objek pada tebing tinggi masih menggunakan renderer pulau 2D yang ada, bukan mesin voxel 3D. Thumbnail dan rumpun rumput menggunakan artwork konsep pilihan.

### Tampilan tanah dasar

Base World memakai warna permukaan rumput yang seragam dan sisi tanah tanpa pola
selang-seling atau garis pembatas antar-block. Pada world bertumpuk, empat tanah
pertama dan rumput di atasnya dikenali sebagai dasar bila empat lapisan pertama
masih `[2,2,2,2]`; block di atasnya tetap memakai variasi warna dan outline sisi.
Format world lama belum menyimpan asal-usul tiap lapisan, jadi pengenalan ini
berdasarkan isi stack, bukan riwayat penempatan. Bila dasar diedit hingga pola
itu berubah, tampilannya kembali mengikuti aturan block biasa. Garis hijau tipis
pada tepi tutup rumput dan lis hijau bawah pulau dihapus; warna sisi tetap
mengikuti pencahayaan. Perubahan ini hanya rendering, tidak menulis ulang world.
Preview dan Android berbagi aturan ini; Android belum diuji pada perangkat.
