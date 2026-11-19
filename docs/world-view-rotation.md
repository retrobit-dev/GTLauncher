# Rencana rotasi tampilan pulau

Permintaan: putar tampilan pulau setiap 90° ke kiri atau kanan di editor dan gameplay preview serta gameplay Android. Koordinat world, inventaris dan rute yang disimpan tidak berubah. Pilihan kamera bukan migrasi format world.

`preview/world-view-rotation.js` dan `WorldViewRotation.java` menyediakan konversi koordinat kamera empat arah, kebalikannya, arah hadap, tapak persegi panjang, sisi pagar dan tapak jembatan. Ini **baru geometri**: belum ada tombol, renderer, pemilihan, atau gerakan yang menggunakannya.

Integrasi harus menerapkan transformasi yang sama pada urutan kedalaman dan tetangga block, air/air terjun, ketinggian, arah serta sprite player/rumah, pagar, jembatan, tanaman, bayangan, kamera, titik sentuh dan jalur. Konversi balik harus dilakukan sebelum tindakan editor atau gameplay menyentuh data. Jangan memutar world JSON atau inventaris. Uji keempat orientasi pada world bertingkat asimetris, objek, air, seleksi dan pergerakan, termasuk pengembalian ke orientasi awal serta kompatibilitas world lama. Android tetap gameplay saja. Pengujian pada perangkat Android masih diperlukan.
