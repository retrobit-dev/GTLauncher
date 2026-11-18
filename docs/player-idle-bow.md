# Animasi idle player

Saat gameplay player diam pada block yang sama selama 3 detik, player membungkuk secara halus selama setengah detik, menahan pose satu detik, lalu tegak kembali dalam setengah detik. Jika tetap diam, siklus dimulai lagi setiap 3 detik (dengan jeda satu detik setelah kembali tegak). Berjalan, berpindah block/world, meninggalkan gameplay atau menyembunyikan aplikasi memulai ulang penghitung. Reduced motion/animasi sistem nonaktif tidak menampilkan gerakan ini. Tidak ada perubahan pada data world.

Ini deformasi ringan pada sprite yang sudah ada, bukan pose baru yang digambar terpisah: tubuh atas sedikit dimiringkan dan direndahkan, sedangkan bagian bawah dan titik kaki tetap. Arah cermin mengikuti sprite, bayangan memakai siluet yang sama, dan renderer air tetap mempertahankan bagian bawah yang terendam. Animasi berjalan tidak diubah. Android menerapkan prinsip yang sama, tetapi belum diuji lewat build atau perangkat.
