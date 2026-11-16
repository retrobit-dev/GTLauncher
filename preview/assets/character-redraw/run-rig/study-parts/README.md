# Potongan studi awal — bukan aset rig final

Delapan PNG adalah bagian lengan dan kaki yang dipisahkan dengan mask poligon manual dari `run-keyposes/front-study.png` dan `back-study.png`. Semua mempertahankan kanvas 1024×1024 dan koordinat sumber, agar mudah dibandingkan dan dilukis kembali. Nama `left`/`right` merujuk sisi gambar, bukan anatomi.

Ini hanya referensi pemisahan visual untuk pengerjaan manual. Pangkal potongan masih terpotong, bagian yang semula tertutup belum tergambar, urutan lapisan dan pivot belum ditetapkan, dan belum ada badan bersih. Beberapa batas dapat mencakup piksel dari bagian berdekatan. Jangan gunakan PNG ini untuk merender karakter atau memutarnya sebagai animasi; perlu gambar ulang daerah tersembunyi, overlap yang aman, pivot terkalibrasi, pose lawan dan pemeriksaan sambungan serta pergerakan sebelum integrasi.

Mask dapat dibuat ulang dengan `PYTHONPATH=<direktori Pillow> python3 tools/character_rig/extract_study_parts.py`. Gambar sumber, sprite runtime, dan world tidak diubah oleh proses ini.
