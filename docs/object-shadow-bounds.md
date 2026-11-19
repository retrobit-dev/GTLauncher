# Batas bayangan objek

Bayangan proyeksi player, rumah, pohon jeruk dan cemara dibatasi dalam koordinat scene: lebar dan tinggi proyeksinya tidak melebihi ukuran sprite yang menjadi batasnya, termasuk saat arah cahaya rendah dan pohon bergoyang. Skala diterapkan seragam terhadap titik pijak agar bentuk dan arah bayangan tetap mengikuti cahaya. Proyeksi player dibatasi pada lebar player berdiri bahkan ketika sprite pose idle lebih lebar. Android juga membatasi bayangan rumah di sekitar titik pijak supaya gabungannya dengan bayangan kontak tetap dalam batas rumah.

Objek Android lain yang belum memiliki proyeksi siluet tetap memakai bayangan kontak kecil; ini bukan klaim bahwa setiap jenis objek memiliki bayangan arah cahaya yang sama. Bayangan awan dan pulau adalah efek lingkungan, bukan objek katalog, dan tidak diubah. Uji geometri mencakup arah cahaya ekstrem, ukuran kecil/besar dan goyangan; tampilan Android belum diuji lewat build atau perangkat.
