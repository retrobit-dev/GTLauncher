# Jalan dan sungai

Objek jalan 1×1 di katalog khusus **Jalan**: tanah, batu, pasir, paving, aspal,
dan sungai. Warna dan detail tiap material mengikuti konsep terpilih
`artwork/road-and-river-concept.png`; sungai mendapat animasi sorotan aliran searah objek. Jalan
sejenis dan setinggi sama di preview menyambung ke tepi tile.

Di Map Editor, pilih world buatan, buka **Objek**, pilih jenis jalan, lalu ketuk
tile untuk menempatkannya; ketuk kembali jalan di tile tersebut untuk menghapusnya.
Penempatan dapat diulang; gunakan Batal untuk berhenti. Undo/Redo tersedia.
Daftar objek juga menyediakan Hapus dan Ganti dengan jenis jalan lain. Jalan
belum mendukung pemilihan langsung pada kanvas, seleksi area, opacity manual,
atau penggantian lintas kategori objek. Jalan tidak ditempatkan di lubang,
terrain air, ramp, rumah, atau tanaman; sungai tidak boleh ditempatkan di
player kecuali ada jembatan. Tile dan block di bawah jalan tidak diubah.

Sungai adalah rintangan routing, bukan block air dan bukan sumber air untuk
alat. Jalan lain dapat dilewati sesuai aturan terrain, ketinggian, pagar dan
objek yang ada. Jembatan yang sudah valid dapat dilalui di atas sungai.
Jalan yang sudah ditempatkan tetap mengikuti permukaan block teratas. Perubahan
terrain/objek yang menyebabkan tapak jalan menjadi tidak valid ditolak.

Format preview **v30** menyimpan `roads` secara terpisah; berkas versi lama tidak
boleh menggantikan world yang memiliki jalan. Android membaca v30, menampilkan
jalan sebagai objek permukaan, dan memperlakukan sungai sebagai air untuk
routing. Browser dan Android tetap memakai penyimpanan terpisah. Pemeriksaan
Java dan browser bukan pengujian APK/perangkat; tampilan Android belum diuji
pada perangkat, dan tampilan permukaan Android tidak identik piksel-per-piksel
dengan preview.

Arah aliran sungai dapat dipilih dengan Putar kiri/kanan sebelum penempatan;
untuk sungai yang sudah ada gunakan Putar pada daftar objek. Arah tersimpan
dan mengikuti rotasi kamera tanpa mengubah orientasi world. Sungai lama tanpa
arah memakai Selatan secara default. Animasi mengikuti pengaturan reduced motion.
Sambungan hanya antara tile sejenis dengan ketinggian sama. Format preview v31
diperlukan untuk menyimpan arah; impor versi lebih lama tidak menggantikan world
yang memiliki arah sungai. Android belum diuji pada perangkat.

Persimpangan jalan sejenis memakai sambungan siku-siku pada grid untuk belokan,
persimpangan T, dan simpang empat, termasuk sudut diagonal yang belum terisi.
Preview dan Android memperhatikan tetangga diagonal sejenis pada ketinggian
yang sama. Ini hanya mengubah rendering; tidak menurunkan terrain atau
mengubah data. Proyeksi isometrik membuat sudut 90° grid terlihat miring di layar.

## Recessed surface rendering

Road surfaces are rendered 1/4 of a block height below their supporting tile; river water is 1/2 below it, with a tinted riverbed drawn a further 1/8 block below the water. Banks are omitted at edges shared with the same material at the same height. This is a visual recess, not excavation of the stored block stack or a change to pathfinding. The player follows the road surface, interpolating between tiles, except on bridge tiles. The riverbed is a visual layer beneath translucent water, not a traversable extra terrain level. World serialization remains unchanged.

## Road direction and visibility

All six road materials can be rotated before placement and from the placed-object list. Rotation changes the tile-local texture (and river flow); it does not rotate the grid, disconnect neighboring same-material roads, or affect routing. Non-river road directions require portable preview format v33; older imports cannot replace a world containing them. Android v33 reads and projects the direction, including when the camera rotates. Unrotated older roads keep their default south orientation. The exposed upper rim is outlined for contrast: the actual road and water offsets are 1/4 and 1/2 block height, respectively, and no terrain stack is excavated. Android rendering has not been checked on a device.

Adjacent roads of the same material at the same elevation share open edges; a small color-matched join covers antialiasing cracks without connecting different materials or elevations. In preview, roads are drawn after terrain so the next tile does not repaint over the lowered neighboring surface. This is still a visual recess: it does not carve the block mesh or create an underground traversable level. Android device rendering remains untested.
