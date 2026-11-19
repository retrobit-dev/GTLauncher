# Batas bayangan objek

Bayangan proyeksi player, pohon jeruk dan cemara dibatasi dalam koordinat scene: lebar dan tinggi proyeksinya tidak melebihi ukuran sprite yang menjadi batasnya, termasuk saat arah cahaya rendah dan pohon bergoyang. Skala diterapkan seragam terhadap titik pijak agar bentuk dan arah bayangan tetap mengikuti cahaya. Proyeksi player dibatasi pada lebar player berdiri bahkan ketika sprite pose idle lebih lebar.

Objek Android lain yang belum memiliki proyeksi siluet tetap memakai bayangan kontak kecil; ini bukan klaim bahwa setiap jenis objek memiliki bayangan arah cahaya yang sama. Bayangan awan dan pulau adalah efek lingkungan, bukan objek katalog, dan tidak diubah. Uji geometri mencakup arah cahaya ekstrem, ukuran kecil/besar dan goyangan; tampilan Android belum diuji lewat build atau perangkat.

## Kontak dan tanaman yang dapat dilewati

Di preview, klip bayangan mengikuti permukaan atas block yang sebenarnya (termasuk
ketinggian dan lubang), bukan diamond pulau datar. Anchor rumah memakai proyeksi
rumah yang sama dengan sprite; akar pohon dan tanaman memiliki bayangan kontak.
Pada Android, anchor proyeksi rumah dan kontak objek mengikuti posisi sprite;
format block bertingkat preview tetap belum didukung Android. Proyeksi bayangan
pada permukaan bertingkat masih pendekatan 2D, bukan simulasi ray tracing.

Rumput liar dan semak dapat dilewati player, tetapi penempatan objek dan rumah
tetap memeriksa okupansi. Rumpun rumput yang bertetangga empat arah dan setinggi
sama diberi jembatan visual; data dan seleksi masing-masing tile tidak digabung.
Android hanya mendukung subset world preview datar sehingga semua tile rumputnya
berada pada ketinggian yang sama. Art cemara lama tetap disimpan; asset redesign
baru dipakai di katalog dan renderer kedua platform.

## Bayangan rumah

Preview dan Android menggunakan tapak fondasi sebagai satu bayangan kontak yang menyatu, bukan proyeksi siluet terpisah. Pergeseran kecil mengikuti azimuth cahaya. Ini pendekatan 2D sederhana, bukan bayangan 3D fisik. Build Android/perangkat belum diuji.

Kontak rumah menggunakan bayangan lembut pada tapak fondasi 3×3 dan sedikit menjorok ke depan, dengan sedikit pergeseran mengikuti arah cahaya; proyeksi siluet terpisah dihilangkan karena tampak melayang. Ukurannya dibatasi agar gabungan tidak melebihi dimensi rumah.
