# Block Rumput di Map Editor

Map Editor hanya tersedia di preview. World baru memakai grid 50×50; world lama tidak diubah ukurannya. Base World terkunci. Pilih world buatan, buka Map Editor, lalu pilih **Block → Rumput**.

- **Tambah block** menempatkan satu block di atas block paling atas, atau pada sel kosong di dalam grid. Tidak memperluas batas grid; tinggi maksimum 32 block per sel. Tinggi satu block sama dengan tinggi pita rumput di sisi pulau.
- **Ganti block** (pensil) mengganti block paling atas, bukan menambah tingkat. Pada sel kosong gunakan Tambah.
- **Hapus block** menghapus block paling atas; sel menjadi kosong jika block terakhir dihapus. **Ambil jenis block** memilih jenis paling atas. **Isi area block** mengganti permukaan yang terhubung dengan jenis dan ketinggian yang sama, atau mengisi area kosong yang terhubung.
- Ketuk permukaan atau sisi block yang terlihat untuk memilih kolomnya. Gerakan seret yang melewati sel yang sama beberapa kali mengubahnya sekali; satu seret merupakan satu langkah Undo/Redo.
- Ketinggian di bawah karakter atau rumah dilindungi, dan rumah hanya dapat dipindahkan ke tapak yang rata. Jika penyimpanan gagal atau data berubah di tab lain, perubahan ditolak.

Gameplay preview menggunakan permukaan teratas: karakter dapat naik atau turun satu tingkat per langkah; sel kosong dan perbedaan dua tingkat atau lebih menghalangi rute. Jalur dapat memutar melalui permukaan bertangga. World datar lama tidak perlu migrasi. Riwayat Undo/Redo berlaku selama sesi tab.

World bertingkat memakai format JSON preview versi 4 saat diekspor; versi Android saat ini **belum mendukung** format ini. Data browser dan Android terpisah dan tidak otomatis tersinkron. Efek pencahayaan, bayangan serta urutan gambar objek pada tebing tinggi masih menggunakan renderer pulau 2D yang ada, bukan mesin voxel 3D. Thumbnail dan rumpun rumput menggunakan artwork konsep pilihan.
