# Bayangan awan

Awan tetap tidak digambar; bayangannya digambar pada permukaan pulau yang ada, termasuk world dengan ukuran grid berbeda. Preview sebelumnya menerapkan transformasi kamera dua kali pada bayangan sehingga bayangan terpotong ke area yang salah; sekarang memakai transformasi yang sama dengan pulau. Pusat awan tersebar di kedua sumbu seluruh grid, mengikuti angin, dan dibungkus pada batas pulau. Pecahan di dua tepi saat melintas adalah satu awan, bukan awan tambahan.

Jumlah awan (kode cuaca WMO): cerah 0–1 sebanyak 3, badai 95–99 sebanyak 7–10 sesuai tutupan, kondisi lain termasuk mendung dan hujan sebanyak 3–6. Ukuran bayangan meningkat dan menyesuaikan ukuran pulau serta jumlah awan; untuk world 50×50, bayangan lebih besar daripada batas lama 4 tile. Bentuk digabung sebelum blur sehingga irisan tidak bertumpuk lebih gelap, dan animasi fade mengikuti pengaturan reduced motion. Saat angin diam, posisi tidak bergerak. Sebaran ke seluruh pulau tidak berarti seluruh tile selalu tertutup bayangan sekaligus.

Aturan dan renderer Android telah disesuaikan untuk subset world yang didukung. Android build dan perangkat belum diuji di lingkungan ini.
