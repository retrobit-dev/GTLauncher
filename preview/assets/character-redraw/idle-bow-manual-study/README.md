# Studi manual pose lelah — belum untuk runtime

Potongan diberi mask poligon manual dari sprite player berdiri saat ini; koordinat dan kanvas tetap 432×800. Sepatu dan sumber asli tidak diubah. Nama `near_hand` merujuk tangan yang paling jelas terlihat, bukan anatomi kanan/kiri. Tujuannya menjaga ukuran kepala, pakaian dan sepatu sebagai referensi saat menggambar pose tangan di lutut.

Ini **belum** rig atau keyframe final. Potongan torso masih mencakup lengan/tali tas, area tersembunyi belum ada, dan tangan jauh sebagian tertutup pouch/tas. Pemutaran potongan secara otomatis menghasilkan celah pinggang dan sambungan tangan terputus. Pose membungkuk dengan dua tangan utuh perlu gambar ulang area yang tertutup, sambungan bahu/siku/pinggang, serta penyesuaian perspektif depan dan belakang sebelum digunakan. Hasil studi ini tidak dihubungkan ke aplikasi; pose sebelumnya masih aktif.

Buat ulang dengan `PYTHONPATH=<direktori Pillow> python3 tools/character_rig/extract_idle_bow_study.py`. Berkas asli dan data world tidak ditulis.
