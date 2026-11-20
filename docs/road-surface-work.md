# Jalan dan sungai — pekerjaan belum selesai

Konsep pilihan berada di `artwork/road-and-river-concept.png`. Modul terpisah
`preview/road-surface.js` berisi enam material, detail gambar masing-masing,
sambungan sisi antar material sejenis, serta validasi record dan helper penempatan
immutable. `tools/test_road_surface.js` menguji fungsi tersebut secara terisolasi.

**Belum terintegrasi dalam aplikasi:** belum ada tombol katalog/editor, renderer
world, penyimpanan/format dan proteksi impor, Undo/Redo, atau perilaku gameplay
untuk jalan ini. Fungsi `isWater` saja tidak menjadikan sungai sebagai penghalang.
Android belum mendukung objek jalan preview baru. Jangan menganggap modul ini
sebagai fitur yang selesai; data world lama tidak diubah oleh pekerjaan ini.
