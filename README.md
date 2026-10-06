# MesraAI - Pasangan Virtual Romantis & Hangat (Android Native)

**MesraAI** adalah aplikasi chatbot pasangan virtual berbasis Android Native (Kotlin & Jetpack Compose) yang dirancang dengan karakter romantis, perhatian, hangat, sedikit manja, dan natural dalam Bahasa Indonesia. Dilengkapi dengan **Avatar Pasangan Interaktif**, **Lip-Sync Otomatis**, **Android Text-to-Speech (id-ID)**, **Memory Lokal Cerdas**, serta **Integrasi Gemini API**.

---

## Fitur Utama

1. **Karakter Pasangan Virtual ("Mesra")**
   - Kepribadian hangat, perhatian, romantis, sedikit manja, bisa bercanda, dan memberi semangat.
   - Menggunakan Bahasa Indonesia yang natural (tidak kaku/seperti robot) dan menyesuaikan panjang jawaban serta gaya bahasa pengguna.
   - Otomatis mendeteksi suasana hati / ekspresi dari percakapan (`NORMAL`, `SMILE`, `HAPPY`, `SHY`, `SAD`).

2. **Avatar Pasangan Hidup & Lip-Sync**
   - Animasi secara real-time menggunakan Jetpack Compose Canvas (ringan, halus 60fps, tanpa video berat):
     - Mata berkedip otomatis (auto-blink).
     - Gerakan kepala & napas halus (idle breathing & head tilt animation).
     - 5 ekspresi wajah dinamis: **Normal**, **Senyum (Smile)**, **Senang (Happy)**, **Malu (Blushing/Shy)**, dan **Sedih (Sad)** + partikel hati melayang.
     - **Lip-Sync Otomatis**: Mulut avatar bergerak membuka-menutup mengikuti ritme suara ketika Text-to-Speech membacakan balasan AI, lalu kembali ke mode idle setelah selesai.

3. **Android Text-to-Speech (`id-ID`) & Voice Input**
   - Menggunakan engine Android Text-to-Speech bawaan dengan bahasa default `id-ID` (tanpa API voice berbayar).
   - Tombol **Aktif/Nonaktifkan Suara (Mute/Unmute)** dan **Putar Ulang Suara** di bagian atas maupun di setiap gelembung pesan AI.
   - Tombol **Floating Voice Input** (Speech-to-Text Bahasa Indonesia) untuk berbicara langsung ke Mesra.

4. **Memory Lokal & Riwayat Percakapan (Room Database & DataStore)**
   - Menyimpan seluruh riwayat chat secara lokal di perangkat agar tidak hilang saat aplikasi ditutup.
   - Menyimpan memori pasangan secara otomatis:
     - Nama / panggilan sayang pengguna.
     - Preferensi percakapan & gaya bahasa.
     - Konteks penting & ringkasan beberapa pesan terakhir.
   - Dilengkapi tombol **Reset Memory** dan **Hapus Chat**.

5. **Konfigurasi Gemini API Key dari Dalam Aplikasi**
   - Tidak menanam API key pribadi secara hardcode di dalam kode sumber.
   - Halaman/Dialog **Pengaturan & API Key** langsung dari dalam aplikasi untuk memasukkan dan menyimpan Gemini API Key secara lokal.
   - Tampilan panduan konfigurasi yang ramah jika API key belum diatur, serta penanganan error yang bersahabat bila koneksi gagal.

---

## Cara Build APK Menggunakan GitHub Actions

Project ini sudah 100% siap di-build secara otomatis di GitHub Actions tanpa perlu mengubah kode atau konfigurasi apa pun:

1. Upload / push seluruh isi repository ini ke GitHub.
2. Buka tab **Actions** di repository GitHub Anda.
3. Pilih workflow **Build APK** di panel sebelah kiri.
4. Klik tombol **Run workflow** → pilih branch → klik **Run workflow**.
5. Tunggu hingga proses build selesai (centang hijau).
6. Scroll ke bagian **Artifacts** di halaman hasil workflow, lalu klik **`MesraAI-debug-apk`** untuk mengunduh file APK.
7. Ekstrak file `.zip` hasil unduhan dan install `app-debug.apk` di perangkat Android Anda.

---

## Teknologi yang Digunakan

- **Bahasa**: Kotlin
- **UI Framework**: Jetpack Compose + Material Design 3 (Dark Mode Romantis Pink/Magenta)
- **Database Lokal**: AndroidX Room Database (`2.7.0`) + DataStore Preferences
- **Networking**: Retrofit2 + OkHttp3 + Kotlinx Serialization
- **AI Engine**: Google Gemini REST API (`gemini-3.5-flash` dengan fallback otomatis)
- **Voice & Audio**: Android `TextToSpeech` (`id-ID`) & `SpeechRecognizer`
- **CI/CD**: GitHub Actions (`.github/workflows/build.yml`)
