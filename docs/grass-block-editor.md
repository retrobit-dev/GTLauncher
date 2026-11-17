# Block Rumput di Map Editor

Map Editor hanya ada di preview. World baru memakai grid 50×50; world berukuran lama tidak diubah ukurannya. Pilih world buatan (Base terkunci), buka Map Editor, pilih **Block → Rumput**, lalu aktifkan **Gambar block**.

- Ketuk atau seret pada sel yang sudah terisi untuk menggantinya dengan Rumput.
- Ketuk atau seret pada sel kosong di dalam grid untuk menambahkan Rumput. Tidak memperluas batas grid.
- **Hapus block** mengosongkan sel; **Ambil jenis block** memilih jenis yang ada; **Isi area block** mengisi area sel sejenis yang tersambung.
- Block yang menopang karakter atau rumah dilindungi dari penghapusan yang membuat posisi tidak valid. Jika penyimpanan gagal, perubahan ditolak.

Editor memakai representasi terrain yang sudah ada: Rumput bernilai 1 dan sel kosong 0. Tidak ada migrasi atau konversi world lama. Perubahan tersimpan di penyimpanan browser dan gameplay preview membaca world tersebut; sel Rumput dapat dilalui. Ini belum menambah mekanik lahan pertanian, panen, atau sinkronisasi otomatis ke Android. Penyimpanan Android dan browser terpisah; impor fisik belum diverifikasi. Bentuk dan tekstur Rumput tetap memakai aset yang ada, bukan desain baru.
