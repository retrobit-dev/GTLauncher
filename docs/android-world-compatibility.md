# Kompatibilitas world Android

Penyimpanan browser dan Android terpisah; tidak ada sinkronisasi otomatis. Editor dan katalog penempatan tetap berada di preview; Android menyediakan pemilih world dan impor untuk gameplay. Impor menggunakan pemilih berkas Android dan meminta konfirmasi bila ID sudah ada.

Android menerima `gtlauncher-preview-world` v1–v24 yang valid, termasuk `blockStacks` sampai 32 block per sel pada grid 20, 25, 50 dan 100. Block, permukaan, rumah, pohon jeruk/cemara, rumput, semak, pagar dan jembatan dipertahankan dalam JSON world. Parser memeriksa versi, material, konsistensi permukaan, tapak rata, tabrakan dan jembatan sebelum menyimpan. Versi 18 menambahkan transparansi manual rumah; versi 19 menambahkan transparansi tanaman, pagar dan jembatan (0–90%, kelipatan 5). Android mempertahankan dan merender nilai tersebut. Versi 20 menambahkan transparansi per block dan per tingkat, termasuk world bertingkat; Android mempertahankan dan merender nilainya. Data tidak dikenal atau tidak valid ditolak tanpa menimpa world atau inventaris. Batas berkas 16 MiB mengakomodasi world besar; jangan ubah nomor versi untuk memaksa impor.

Format `gtlauncher-world` lama dan `gtlauncher-preview-world` berbeda dan tidak saling menimpa dengan ID sama. Guard downgrade mencegah format lama menghapus block bertingkat, block baru, transparansi rumah atau objek yang tidak bisa direpresentasikan. Saat terrain digarap, hanya block teratas yang diubah; lapisan bawah tetap utuh.

## Gameplay terrain bertingkat

Renderer Android menggambar kolom per level dan menempatkan player, rumah, tanaman, pagar, jembatan, rute, kontak bayangan dan kamera pada tinggi permukaannya. Tap memilih permukaan atau sisi block yang terlihat. Ketinggian gerak dianimasikan antara dua tile; rute membatasi perbedaan tinggi satu block dan mempertahankan aturan jembatan/pagar. Sambungan rumput dan pagar hanya dibentuk pada ketinggian yang sama. Air memakai tetangga pada level sama dan air terjun visual mengalir dari air yang lebih tinggi ke yang lebih rendah. Bayangan awan diklip pada permukaan yang ditinggikan.

Tes Java mencakup data bertingkat, versi, penyimpanan, garap, downgrade, pergerakan, jembatan, picking, ketinggian, sambungan dan aturan air. Seluruh sumber Java dikompilasi terhadap stub API Android 35 dan R buatan untuk pemeriksaan tipe; ini **bukan build APK**. Renderer, performa world besar, tampilan air/bayangan, sentuhan dan kamera belum diuji di emulator atau perangkat Android dan mungkin masih memerlukan penyesuaian visual/performa.

## Ramp preview (versi 21)

Preview dapat menempatkan ramp di atas block penyangga melalui alat Ramp. Pilih material, letakkan atau pilih ramp dan gunakan tombol putar atau hapus. Ramp tersimpan sebagai `ramps` dengan koordinat, arah dan material, menggunakan format ekspor preview v21. Block penyangga tidak dapat diubah sebelum ramp dihapus; tapak rumah, tanaman, pagar, jembatan dan player tidak dapat ditimpa saat penempatan. Undo/Redo mendukung perubahan ramp. Impor format lebih lama tidak boleh menimpa world yang memiliki ramp.

Kode Android mendukung impor v21, rendering ramp, pemilihan permukaan, routing, gerak di lereng, deteksi ramp air untuk player, dan guard downgrade. Bidang ramp air memiliki batas dan animasi aliran menurun; sambungan kompleks belum diverifikasi pada perangkat. Versi Android lama menolak v21; jangan mengganti nomor versi untuk memaksa impor. Tes model Java dan pemeriksaan tipe API lulus, tetapi APK, tampilan, air dan sentuhan ramp **belum diuji di emulator atau perangkat**. Rotasi pulau dan fade otomatis block atas objek juga belum selesai.

## Lapisan Base World bawaan

Base World bawaan yang tidak diedit memiliki empat block tanah di bawah satu block rumput pada setiap sel 50×50. Sejak perubahan ini, **world baru** di preview (termasuk Buat Home baru) menyalin susunan itu sebagai stack yang dapat diedit, masing-masing sel independen. World buatan dan impor yang sudah ada tidak dimigrasi; inventaris tidak diubah. Stack baru diekspor dengan format preview v4 yang dapat dibaca parser Android yang mendukung stack. Base bawaan masih memakai representasi visual khusus, sedangkan salinannya memakai renderer stack biasa; kesamaan tampilannya belum diverifikasi sepenuhnya. Android hanya gameplay dan tampilan/impor belum diuji pada perangkat atau emulator.

## Jembatan lubang (versi 23)

Jembatan baru 3×1, 4×1 dan 5×1 memiliki ujung darat rata dan bagian tengah satu tingkat lebih rendah. Jalur karakter tetap selebar satu block; bentuk visual sisi menjorok keluar. World lama dan jembatan v22 tidak dimigrasi. Impor, rute, tinggi pijakan dan guard downgrade v23 diuji dengan model Java dan pemeriksaan tipe; **belum ada pengujian APK atau perangkat Android**.

## Jembatan di atas air (versi 24)

Jembatan baru dapat melintasi air dengan ketinggian data sama seperti kedua
ujung darat, atau lubang/air satu tingkat di bawahnya. Permukaan air diturunkan
secara visual saja; isi dan tinggi stack lama tetap. `overWater` mensyaratkan
format v24; impor format lebih lama tidak dapat menimpa data ini. Tes Java dan
browser sudah dijalankan; APK dan perangkat Android belum diuji.

## Penyesuaian v25

Ketinggian air visual kembali seperti semula. Jembatan baru dapat ditempatkan pada berbagai jenis dan ketinggian terrain, termasuk ramp dan lubang, dengan tinggi dek tetap pada tile awal; batas grid dan larangan menimpa objek tetap berlaku. Data lama tidak dimigrasi. World dengan jembatan bebas diekspor sebagai v25 dan tidak dapat ditimpa format sebelumnya. Kode parser dan rute Android diuji pada model Java, bukan APK atau perangkat.

Preview world v26 menambahkan `centered:true` untuk jembatan bebas dengan dek dua tile yang terpusat. Android versi ini membaca, memvalidasi dan menggambar data tersebut; v25 dan sebelumnya tetap dapat dibaca tanpa mengubah ukuran jembatan lama. Impor format lebih lama tidak boleh menimpa world yang sudah memiliki jembatan terpusat. Pemeriksaan Java bukan pengujian APK atau perangkat.

Preview world v27 menambahkan `inset:true` untuk jembatan bebas satu tile dengan rail dan tiang di dalam tapak. Jembatan format lama termasuk v26 tetap dipertahankan; versi sebelum v27 tidak dapat menimpa world yang memiliki penanda ini. Android belum diuji di perangkat.

Preview world v28 menambahkan `straight:true` untuk jembatan datar yang melebar ¼ tile pada setiap ujung. Android versi ini membaca, memvalidasi, merotasi dan menggambarnya, tanpa mengubah geometri world v27 dan sebelumnya; impor format yang lebih lama tidak dapat menimpa world dengan jembatan tersebut. Pengujian Java/typecheck bukan pengujian APK atau perangkat.

Reset jembatan satu kali: setelah pembaruan, data world preview yang tersimpan di perangkat diproses secara terpisah dari browser. Cadangan array world disimpan di preferensi privat `worlds_before_bridge_redesign_v1` dalam transaksi yang sama; belum ada tombol pemulihan untuk cadangan ini. World yang tidak dapat dibaca atau karakter di lubang tanpa tile aman mencegah reset, sehingga data tetap utuh. Jembatan yang diimpor setelah reset tidak dihapus otomatis. Belum diuji di perangkat.

Base World memiliki lima lapisan dasar; stack tambahan tidak lagi memiliki batas tinggi aplikasi. Android tidak memangkas world tersimpan dan menerima impor dengan stack lebih tinggi; kapasitas memori dan perangkat tetap membatasi ukuran praktis. Cadangan migrasi sebelumnya tetap berada di preferensi privat `worlds_before_five_blocks_v1`, tanpa UI pemulihan. Pengujian pada perangkat belum dilakukan.

Preview format v29 mendukung rongga dan block melayang: 0 di tengah `blockStacks` adalah ruang kosong, bukan block; setiap stack tidak kosong harus berakhir dengan block. Versi lebih lama ditolak bila mengandung rongga dan tidak boleh mengganti world v29 yang memerlukannya. Android membaca dan menggambar block melayang, tetapi gameplay masih mengikuti permukaan teratas dan tidak dapat berjalan melalui rongga di bawahnya. Belum diuji pada perangkat.
