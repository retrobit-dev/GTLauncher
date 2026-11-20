# Block Rumput di Map Editor

Map Editor hanya tersedia di preview. World baru memakai salinan terrain Base World 50×50: empat block tanah dan satu block rumput per tile, yang dapat diedit secara independen. World lama tidak dimigrasi atau diubah ukurannya. Base World terkunci. Pilih world buatan, buka Map Editor, lalu pilih **Block → Rumput**. Pemilih block menampilkan nama lengkap dan thumbnail; pada layar kecil panel alat dapat digulir.

- **Tambah block** menempatkan satu block di atas block paling atas, atau pada sel kosong di dalam grid. Tidak memperluas batas grid; tinggi maksimum 32 block per sel. Tinggi satu block sama dengan tinggi pita rumput di sisi pulau.
- **Ganti block** (pensil) mengganti block paling atas, bukan menambah tingkat. Pada sel kosong gunakan Tambah.
- **Hapus block** menghapus block paling atas; sel menjadi kosong jika block terakhir dihapus. **Ambil jenis block** memilih jenis paling atas. **Isi area block** mengganti permukaan yang terhubung dengan jenis dan ketinggian yang sama, atau mengisi area kosong yang terhubung.
- Ketuk permukaan atau sisi block yang terlihat untuk memilih kolomnya. Gerakan seret yang melewati sel yang sama beberapa kali mengubahnya sekali; satu seret merupakan satu langkah Undo/Redo.
- Ketinggian di bawah karakter atau rumah dilindungi, dan rumah hanya dapat dipindahkan ke tapak yang rata. Jika penyimpanan gagal atau data berubah di tab lain, perubahan ditolak.

Gameplay preview menggunakan permukaan teratas: karakter dapat naik atau turun satu tingkat per langkah; sel kosong dan perbedaan dua tingkat atau lebih menghalangi rute. Jalur dapat memutar melalui permukaan bertangga. World datar lama tidak perlu migrasi. Riwayat Undo/Redo berlaku selama sesi tab.

World bertingkat memakai format JSON preview versi 4 saat diekspor; versi Android saat ini dapat mencoba mengimpor format ini; dukungan v21 tersedia tetapi belum diuji pada perangkat. Data browser dan Android terpisah dan tidak otomatis tersinkron. Efek pencahayaan, bayangan serta urutan gambar objek pada tebing tinggi masih menggunakan renderer pulau 2D yang ada, bukan mesin voxel 3D. Thumbnail dan rumpun rumput menggunakan artwork konsep pilihan.

### Tampilan tanah dasar

Rumput permukaan kembali menggunakan warna berselang-seling dengan batas tipis
antar-tile. Sisi tanah bawah memakai satu warna yang sama di kiri dan kanan;
garis hijau dari sisi rumput dasar yang tertutup block lain diganti warna tanah.
Block tambahan tetap memiliki variasi warna dan outline sisi. Aturan rendering
berlaku pada Base World dan world lama maupun baru, tanpa mengubah data world.

Format lama belum mencatat asal-usul lapisan. Tingkat nol dianggap dasar; empat
lapisan awal `[2,2,2,2]` serta rumput di atasnya dikenali sebagai template dasar.
Karena ini berdasarkan isi stack, lapisan hasil edit yang menyerupai template
juga mengikuti tampilan dasar. Perubahan belum diuji di perangkat Android.

Pada world lama dengan rumput dasar yang sudah tertutup block, renderer tidak
lagi menggambar tutup, garis, atau detail rumput pada lapisan tertutup itu;
permukaannya mengikuti warna tanah agar tidak muncul garis hijau di tengah sisi
pulau. Tanah dasar digelapkan, sementara rumput teratas tetap berpola. Tes
`tools/test_soil_green_seam_browser.js` memeriksa stack lama `[1,2,2,2,1]`,
garis pada rumput yang terlihat, serta memastikan data world tidak berubah.

Garis hijau yang masih terlihat pada Home ternyata berasal dari garis tepi pulau
terpisah yang digambar pada tinggi lama, bukan hanya dari rumput tertutup. Garis
tersebut dihapus tanpa menghapus batas tile rumput. Tes
`tools/test_soil_rim_pixels_browser.js` membuat Home baru dan memeriksa piksel
pada posisi garis itu; pengujian tidak mengubah data world.
