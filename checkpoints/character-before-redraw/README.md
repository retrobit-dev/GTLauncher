# Checkpoint sebelum gambar ulang karakter

Snapshot ini diambil dari branch pada commit `16c1f17`. Ini bukan checkpoint sebelumnya yang hilang dari workspace. Berkas bersama disimpan secara utuh; perubahan yang terjadi setelah seal akan menyebabkan undo berhenti, bukan tertimpa.

Gambar ulang belum aktif, jadi checkpoint **belum disegel**. Setelah integrasi selesai dan daftar FILES pada `tools/character_redraw_checkpoint.py` diperluas untuk semua file yang diubah, segel sekali dengan:

```sh
python3 tools/character_redraw_checkpoint.py seal
```

Untuk mengembalikan berkas yang dicakup:

```sh
python3 tools/character_redraw_checkpoint.py undo
```

Undo memeriksa semua berkas dan integritas backup terlebih dahulu; jika ada konflik, tidak ada berkas yang dipulihkan. Aset baru yang ditambahkan harus ditangani terpisah. Jangan menyegel ulang setelah perubahan lain.
