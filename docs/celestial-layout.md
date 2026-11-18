# Benda langit dan bayangan pulau

Matahari dan bulan tetap disembunyikan sementara pada preview dan Android; renderer mereka disimpan untuk pemulihan kemudian, bukan diaktifkan ulang. Geometri renderer preview mengikuti aturan Android: busur visual berpusat pada pulau, kanan → atas → kiri, lebar radius 300 dan tinggi 175 unit; posisi bulan dihitung dari orbitnya sendiri. Diameter matahari satu lebar tile dan bulan setengahnya. Keduanya mengikuti transformasi pulau/zoom ketika kelak ditampilkan. Pencahayaan dan bayangan objek saat ini masih memakai posisi cahaya virtual yang sudah ada, bukan busur visual tersebut.

Bintang dekoratif kini juga digambar di preview pada busur dan transformasi pulau/zoom seperti Android; bintang bukan peta astronomi aktual, meredup saat siang atau berawan, dan dapat berada di luar layar tergantung kamera/zoom. Bayangan pulau preview mengikuti skala visual berdasarkan jarak cahaya dan tepi bawahnya 50 unit dari pusat tile paling bawah, seperti Android. Angka tersebut adalah unit scene sebelum zoom, bukan piksel layar.

Android tidak diubah untuk geometri ini dan belum diuji dengan build atau perangkat dalam perubahan ini. Tampilan matahari/bulan setelah diaktifkan kembali masih perlu diuji visual bersama pengguna.
