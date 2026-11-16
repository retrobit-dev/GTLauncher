# Lengan uji — belum untuk game

Dua gambar sumber adalah eksperimen membuat lengan utuh berdasarkan studi karakter; PNG transparan disiapkan dari masing-masing sumber. Pratinjau `run-rig-review.html` dapat memutar satu lengan per arah di sekitar titik bahu **perkiraan**. Ini membuktikan pemuatan dan transformasi dasar, bukan rig produksi: lengan masih satu bagian kaku, sikunya tidak bersendi, belum ada pasangan lengan, badan tanpa anggota tubuh, lapisan penutup, kaki terpisah yang lengkap atau sambungan rapi. Pakaian dan proporsi masih perlu dicocokkan dengan sprite aktual dan setiap pose. Jangan gunakan sebagai frame runtime.

Percobaan gambar kaki ditolak karena masih berisi bagian dari kedua kaki; pose lawan belum dibuat. Tidak ada perubahan pada aset karakter di game. Untuk membuat ulang transparansi: `PYTHONPATH=<Pillow> python3 tools/character_rig/prepare_draft_arms.py`.
