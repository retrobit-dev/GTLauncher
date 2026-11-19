# Kompatibilitas world Android

Penyimpanan browser dan Android terpisah; tidak ada sinkronisasi otomatis. Editor dan katalog penempatan tetap berada di preview; Android menyediakan pemilih world dan impor untuk gameplay. Impor menggunakan pemilih berkas Android dan meminta konfirmasi bila ID sudah ada.

Android menerima `gtlauncher-preview-world` v1–v21 yang valid, termasuk `blockStacks` sampai 32 block per sel pada grid 20, 25, 50 dan 100. Block, permukaan, rumah, pohon jeruk/cemara, rumput, semak, pagar dan jembatan dipertahankan dalam JSON world. Parser memeriksa versi, material, konsistensi permukaan, tapak rata, tabrakan dan jembatan sebelum menyimpan. Versi 18 menambahkan transparansi manual rumah; versi 19 menambahkan transparansi tanaman, pagar dan jembatan (0–90%, kelipatan 5). Android mempertahankan dan merender nilai tersebut. Versi 20 menambahkan transparansi per block dan per tingkat, termasuk world bertingkat; Android mempertahankan dan merender nilainya. Data tidak dikenal atau tidak valid ditolak tanpa menimpa world atau inventaris. Batas berkas 16 MiB mengakomodasi world besar; jangan ubah nomor versi untuk memaksa impor.

Format `gtlauncher-world` lama dan `gtlauncher-preview-world` berbeda dan tidak saling menimpa dengan ID sama. Guard downgrade mencegah format lama menghapus block bertingkat, block baru, transparansi rumah atau objek yang tidak bisa direpresentasikan. Saat terrain digarap, hanya block teratas yang diubah; lapisan bawah tetap utuh.

## Gameplay terrain bertingkat

Renderer Android menggambar kolom per level dan menempatkan player, rumah, tanaman, pagar, jembatan, rute, kontak bayangan dan kamera pada tinggi permukaannya. Tap memilih permukaan atau sisi block yang terlihat. Ketinggian gerak dianimasikan antara dua tile; rute membatasi perbedaan tinggi satu block dan mempertahankan aturan jembatan/pagar. Sambungan rumput dan pagar hanya dibentuk pada ketinggian yang sama. Air memakai tetangga pada level sama dan air terjun visual mengalir dari air yang lebih tinggi ke yang lebih rendah. Bayangan awan diklip pada permukaan yang ditinggikan.

Tes Java mencakup data bertingkat, versi, penyimpanan, garap, downgrade, pergerakan, jembatan, picking, ketinggian, sambungan dan aturan air. Seluruh sumber Java dikompilasi terhadap stub API Android 35 dan R buatan untuk pemeriksaan tipe; ini **bukan build APK**. Renderer, performa world besar, tampilan air/bayangan, sentuhan dan kamera belum diuji di emulator atau perangkat Android dan mungkin masih memerlukan penyesuaian visual/performa.

## Ramp preview (versi 21)

Preview dapat menempatkan ramp di atas block penyangga melalui alat Ramp. Pilih material dan arah naik; ketuk ramp yang sama dengan material dan arah yang sama untuk menghapusnya. Ramp tersimpan sebagai `ramps` dengan koordinat, arah dan material, menggunakan format ekspor preview v21. Block penyangga tidak dapat diubah sebelum ramp dihapus; tapak rumah, tanaman, pagar, jembatan dan player tidak dapat ditimpa saat penempatan. Undo/Redo mendukung perubahan ramp. Impor format lebih lama tidak boleh menimpa world yang memiliki ramp.

Kode Android mendukung impor v21, rendering ramp, pemilihan permukaan, routing, gerak di lereng, deteksi ramp air untuk player, dan guard downgrade. Aliran serta sambungan air khusus pada bidang ramp belum diselesaikan. Versi Android lama menolak v21; jangan mengganti nomor versi untuk memaksa impor. Tes model Java dan pemeriksaan tipe API lulus, tetapi APK, tampilan, air dan sentuhan ramp **belum diuji di emulator atau perangkat**. Rotasi pulau dan fade otomatis block atas objek juga belum selesai.
