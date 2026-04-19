# Tubes MAD (IF3210-24) 2026 - MS1 - K01 - FAR

## Deskripsi Aplikasi
Nimons360 adalah aplikasi Android yang dirancang untuk membantu manajemen dan pelacakan keluarga secara real-time, terinspirasi dari fungsionalitas utama aplikasi Life360. Aplikasi ini memungkinkan pengguna untuk membuat keluarga, memantau lokasi anggota, serta mengelola daftar keluarga dengan fitur pencarian dan penyematan.

## Fitur Utama
- **Autentikasi Pengguna** : Sistem Login dan Logout yang aman.
- **Manajemen Keluarga** : Membuat, bergabung, meninggalkan, dan melihat detail keluarga.
- **Pelacakan Lokasi** : Integrasi Map dan GPS untuk memantau anggota keluarga secara real-time.
- **Interaksi Interaktif** : Fitur pencarian, filter, dan penyematan keluarga.
- **Profil Pengguna** : Pengelolaan informasi akun dan sesi.

## Library yang Digunakan
- **Material Components** : Implementasi standar komponen antarmuka Material Design 3.
- **Jetpack Compose** : Framework modern untuk membangun UI deklaratif secara penuh.
- **Retrofit & OKHttp** : Client HTTP untuk berkomunikasi dengan REST API secara efisien.
- **GSON & Retrofit Converter GSON** : Serialisasi dan deserialisasi data JSON menjadi objek model.
- **Room Database** : Sistem manajemen database lokal (ORM) untuk penyimpanan data persisten.
- **Kotlinx Coroutines** : Library untuk menangani operasi asinkron dan concurrency.
- **Lifecycle (ViewModel, Runtime, Compose)** : Manajemen state aplikasi dan data UI.
- **Security Crypto** : Implementasi enkripsi untuk penyimpanan data sensitif (EncryptedSharedPreferences).
- **Coil & Coil Compose** : Library pemuatan gambar yang dioptimalkan untuk performa tinggi, termasuk dukungan Compose.
- **OSMDroid** : Integrasi dan rendering peta interaktif menggunakan sumber OpenStreetMap.
- **Google Play Services Location** : API untuk pengambilan data lokasi presisi dan koordinat GPS.
- **SwipeRefreshLayout** : Komponen UI untuk fitur "pull down to refresh" pada layout XML.

## Pembagian Kerja Anggota Kelompok
| Spesifikasi | UI Framework | Kontributor |
| :--- | :--- | :---: |
| **Wajib - Auth** | XML | 13523035 |
| **Wajib - Header & Navbar** | XML | 13523035 |
| **Wajib - Home** | XML | 13523035 |
| **Wajib - Families** | XML (Create Family) & Jetpack Compose (Other) | 13523021 |
| **Wajib - Map** | Jetpack Compose | 13523005 |
| **Wajib - Profile** | XML | 13523035 |
| **Wajib - Network Sensing** | Jetpack Compose | 13523035 |
| **Wajib - OpenAPI** | None | 13523035 |
| **Wajib - README** | None | 13523021 |
| **Bonus - Search Family** | Jetpack Compose | 13523021 |
| **Bonus - Internet Status** | Jetpack Compose | 13523005 |
| **Bonus - Livestreaming** | Jetpack Compose | 13523005 |
| **Bonus - Mark Favorite Location** | Jetpack Compose | 13523005 |
| **Bonus - Accessibility Testing** | None | 13523035 |

## Waktu Pengerjaan
| NIM | Jam Persiapan | Jam Pengerjaan | Total Jam |
| :---: | :---: | :---: | :---: |
| **13523005** | 6 Jam | 30 Jam | 36 Jam |
| **13523021** | 6 Jam | 30 Jam | 36 Jam |
| **13523035** | 6 Jam | 30 Jam | 36 Jam |

## Screenshot Aplikasi

### Wajib - Preview Aplikasi
- **Logo (Display)** <img src="screenshot/logo1.jpg" width="250">

### Wajib - Auth
- **Login (Input)** <img src="screenshot/auth1.jpg" width="250">
- **Login (Success)** <img src="screenshot/auth2.jpg" width="250">
- **Login (Failed)** <img src="screenshot/auth3.jpg" width="250">
- **Logout (Display)** <img src="screenshot/auth4.jpg" width="250">

### Wajib - Header & Navbar
- **Header (Display)** <img src="screenshot/nav1.jpg" width="250">
- **Navbar (Display)** <img src="screenshot/nav2.jpg" width="250">

### Wajib - Home
- **My Families (Display)** <img src="screenshot/home1.jpg" width="250">
- **Discover Families (Display)** <img src="screenshot/home2.jpg" width="250">

### Wajib - Families
- **Create Family (Input)** <img src="screenshot/families1.jpg" width="250">
- **Create Family (Success)** <img src="screenshot/families2.jpg" width="250">
- **Create Family (Failed)** <img src="screenshot/families3.jpg" width="250">
- **Family Detail (Joined)** <img src="screenshot/families4.jpg" width="250">
- **Family Detail (Not Joined)** <img src="screenshot/families5.jpg" width="250">
- **Join Family (Input)** <img src="screenshot/families6.jpg" width="250">
- **Join Family (Error Minimum Character)** <img src="screenshot/families7.jpg" width="250">
- **Join Family (Error Wrong Code)** <img src="screenshot/families8.jpg" width="250">
- **Leave Family (Display)** <img src="screenshot/families9.jpg" width="250">
- **Pinned Family (Display)** <img src="screenshot/families10.jpg" width="250">
- **Pinned Family (Add)** <img src="screenshot/families11.jpg" width="250">
- **Pinned Family (Delete)** <img src="screenshot/families12.jpg" width="250">
- **Filter Family (All)** <img src="screenshot/families13.jpg" width="250">
- **Filter Family (My Families > 0)** <img src="screenshot/families14.jpg" width="250">
- **Filter Family (My Families = 0)** <img src="screenshot/families15.jpg" width="250">

### Wajib - Map
- **Map (Display)** <img src="screenshot/map1.jpg" width="250">
- **Family Location (Display)** <img src="screenshot/map2.jpg" width="250">
- **Phone Orientation (Display)** <img src="screenshot/map3.jpg" width="250">
- **User Information (Display)** <img src="screenshot/map4.jpg" width="250">

### Wajib - Profile
- **Profile (Display)** <img src="screenshot/profile1.jpg" width="250">
- **Rename (Success)** <img src="screenshot/profile2.jpg" width="250">
- **Rename (Failed)** <img src="screenshot/profile3.jpg" width="250">

### Wajib - Network Sensing
- **Connected (Display)** <img src="screenshot/network1.jpg" width="250">
- **Disconnected (Display)** <img src="screenshot/network2.jpg" width="250">

### Wajib - OpenAPI
- **Documentation (Display)** <img src="screenshot/openapi1.jpg" width="250">

### Bonus - Search Family
- **All Families (Found)** <img src="screenshot/search1.jpg" width="250">
- **All Families (Not Found)** <img src="screenshot/search2.jpg" width="250">
- **My Families (Found)** <img src="screenshot/search3.jpg" width="250">
- **My Families (Not Found)** <img src="screenshot/search4.jpg" width="250">

### Bonus - Internet Status
- **Internet Status (Display)** <img src="screenshot/internet1.jpg" width="250">

### Bonus - Mark Favorite Location
- **Map (Display)** <img src="screenshot/favorite1.jpg" width="250">
- **Add Favorite Location (Success)** <img src="screenshot/favorite2.jpg" width="250">
- **Add Favorite Location (Failed)** <img src="screenshot/favorite3.jpg" width="250">
- **Rename Favorite Location (Success)** <img src="screenshot/favorite4.jpg" width="250">
- **Rename Favorite Location (Failed)** <img src="screenshot/favorite5.jpg" width="250">
- **Delete Favorite Location (Display)** <img src="screenshot/favorite6.jpg" width="250">

### Bonus - Accessibility Testing
- Rincian Hasil Test 1 <img src="screenshot/accessibility1.jpg" width="250">
- Rincian Hasil Test 2 <img src="screenshot/accessibility2.jpg" width="250">
- Rincian Hasil Test 3 <img src="screenshot/accessibility3.jpg" width="250">

### Other
Secara lebih lengkap, smua dokumentasi visual aplikasi dapat ditemukan pada direktori berikut : `./screenshot`
