# Rumah player

Konsep rumah awal terpilih disimpan di `preview/assets/character-redraw/house-redesign-concept.png`. Tampilan belakang adalah interpretasi baru dari konsep tersebut. `tools/build_player_house.py` menghasilkan sprite transparan dari kedua sumber tanpa menimpa aset rumah lama. Tampilan depan dan belakang standar juga disertakan di Android; desain rumah Android yang sudah ada dengan ekstensi wide tidak diubah. Belum ada mekanisme atau syarat upgrade, interior, ataupun masuk rumah.

Pada Map Editor preview, buka **Objek → Katalog objek → Rumah player → Tempatkan rumah**, lalu ketuk tile untuk sudut pertama tapak 3×3. Satu world buatan dapat memiliki satu rumah player; Base World tetap terkunci. Area harus berada di dalam world, rata, tanpa air atau lubang dan tidak ditempati player. Escape membatalkan; penempatan mendukung undo/redo dalam sesi editor. Rumah yang sudah ada memakai tampilan baru tanpa mengubah posisi, arah, skala, atau data terrain. Pemindahan dan interaksi tangan menggunakan sistem rumah sebelumnya; rumah tidak dapat diambil ke tas. Daftar katalog objek lain belum tersedia.

Peta preview dan Android tetap tersimpan terpisah. Penempatan melalui katalog hanya tersedia di preview, bukan editor native Android. Android memakai sprite baru untuk rumah standar tetapi build dan perangkat Android belum diverifikasi; world bertingkat JSON v4 preview belum didukung Android.

## Desain sederhana

Rumah standar kini menggunakan konsep `house-player-simple-concept.png`, dengan interpretasi belakang terpisah dan pintu yang lebih tinggi dari player pada skala bawaan. Aset dan konsep rumah sebelumnya tetap disimpan. Aset Android rumah standar juga diperbarui, tetapi belum dibangun atau diuji pada perangkat. Desain wide lama belum didesain ulang; fitur upgrade belum tersedia.

Tampilan depan dan belakang rumah sederhana disejajarkan pada titik kontak sudut pondasi yang sama sebelum dicerminkan untuk arah lain. Dengan demikian rotasi mempertahankan titik pijak, posisi tapak dan skala; hasil asli sebelum disejajarkan tetap tersedia. Sprite Android standar diselaraskan juga, tetapi belum diuji melalui build atau perangkat. Rumah Android desain wide yang lama tidak diubah.
