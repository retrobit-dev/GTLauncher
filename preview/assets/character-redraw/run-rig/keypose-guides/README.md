# Panduan gambar untuk pose lari

Empat PNG ini adalah **diagram sendi**, bukan frame animasi: tampak `front` dan `back`, masing-masing langkah `a` dan `b`. Merah adalah anggota tubuh dekat kamera dan biru yang jauh. Karakter diam yang samar hanya acuan desain. Posisi sendi masih perkiraan dan belum disesuaikan secara manual dengan artwork; jangan menelusuri diagram ini sebagai anatomi final.

Untuk ilustrator/animator: buat pose utuh dan konsisten untuk kedua langkah pada tiap arah. Jaga topi, wajah, pakaian, tas, skala dan arah karakter yang disetujui; pastikan dua tangan dan dua kaki utuh, lengan berlawanan dengan kaki, dan perubahan occlusion wajar. Tambahkan pose kontak, passing dan melayang serta in-between untuk siklus yang halus; samakan anchor tanah dan dimensi semua frame. Pratinjau tiap frame dan siklus kedua arah sebelum ekspor. Hasil akhir perlu latar transparan, pivot/anchor sama, serta aset cocok untuk preview dan Android. Jangan gunakan diagram ini sebagai sprite game.

Dibuat ulang dengan `PYTHONPATH=<Pillow> python3 tools/character_rig/export_keypose_guides.py` dari `joints.json` dan sprite karakter aktif. Panduan ini belum membuktikan bahwa anatomi atau gerak sudah siap untuk animasi.
