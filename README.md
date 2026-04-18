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
- **Jetpack Compose** : Untuk membangun UI yang modern dan deklaratif.
- **Material Design 3** : Implementasi komponen UI standar industri.
- **Retrofit & Gson** : Untuk menangani komunikasi jaringan dan parsing data API.
- **Room Database** : Untuk penyimpanan data lokal dan fitur penyematan keluarga.
- **Coil**: Untuk pemuatan gambar dan ikon keluarga secara asinkron.
- **Lifecycle & ViewModel**: Untuk manajemen state aplikasi yang robust.

## Screenshot
### Wajib - Preview Aplikasi
- Logo (Display)
### Wajib - Auth
- Login (Input)
- Login (Success)
- Login (Failed)
- Logout (Display)
### Wajib - Header & Navbar
- Header (Display)
- Navbar (Display)
### Wajib - Home
- My Families (Display)
- Discover Families (Display)
### Wajib - Families
- Create Family (Input)
- Create Family (Success)
- Create Family (Failed)
- Family Detail (Joined)
- Family Detail (Not Joined)
- Join Family (Input)
- Join Family (Error Minimum Character)
- Join Family (Error Wrong Code)
- Leave Family (Display)
- Pinned Family (Display)
- Pinned Family (Add)
- Pinned Family (Delete)
- Filter Family (All)
- Filter Family (My Families (>0))
- Filter Family (My Families (=0))
### Wajib - Map
- Map (Display)
- Family Location (Display)
- Phone Orientation (Display)
- User Information (Display)
### Wajib - Profile
- Profile (Display)
- Rename (Success)
- Rename (Failed)
### Wajib - Network Sensing
- Connected (Display)
- Disconnected (Display)
### Wajib - OpenAPI
- Documentation (Display)
### Bonus - Search Family
- All Families (Found)
- All Families (Not Found)
- My Families (Found)
- My Families (Not Found)
### Bonus - Internet Status
- Internet Status (Display)
### Bonus - Mark Favorite Location
- Map (Display)
- Add Favorite Location (Success)
- Add Favorite Location (Failed)
- Rename Favorite Location (Success)
- Rename Favorite Location (Failed)
- Delete Favorite Location (Display)

### Other
Secara lebih lengkap, smua dokumentasi visual aplikasi dapat ditemukan pada direktori berikut : `./screenshot`

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
| **Bonus - Search Family** | Jetpack Compose | 13523021 |
| **Bonus - Internet Status** | Jetpack Compose | 13523005 |
| **Bonus - Mark Favorite Location** | Jetpack Compose | 13523005 |

## Waktu Pengerjaan

| NIM | Jam Persiapan | Jam Pengerjaan | Total Jam |
| :---: | :---: | :---: | :---: |
| **13523005** | 6 Jam | 30 Jam | 36 Jam |
| **13523021** | 6 Jam | 30 Jam | 36 Jam |
| **13523035** | 6 Jam | 30 Jam | 36 Jam |
