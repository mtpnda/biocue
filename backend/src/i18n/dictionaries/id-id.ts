import { Dictionary } from "../types.ts";

export const idIdDictionary: Dictionary = {
  locale: "id-id",
  localeName: "Bahasa Indonesia",
  flag: "🇮🇩",

  meta: {
    title: "BioCue — Prediksi Sensory Overload & Isyarat Bio-Sinyal",
    description: "Sistem jam tangan pintar Wear OS dan aplikasi pendamping Android gratis & open-source untuk memprediksi lonjakan sensory overload 5–15 menit lebih awal dengan desain bebas cemas dan privasi lokal mutlak.",
  },

  nav: {
    features: "Fitur",
    howItWorks: "Cara Kerja",
    simulator: "Simulator Sensorik",
    faq: "Tanya Jawab",
    support: "Bantuan & Kontak",
    download: "Unduh Aplikasi",
    getApp: "Unduh Gratis →",
  },

  hero: {
    badge: "🌱 100% Gratis & Teknologi Bantu Open-Source",
    titleLine1: "Perlindungan Sensory Overload",
    titleHighlight: "Sebelum Meltdown Memuncak",
    titleLine2: "Dengan Ketenangan di Wear OS.",
    sub: "BioCue memantau sistem saraf otonom secara hening di smartwatch Anda untuk memprediksi sensory overload 5–15 menit lebih awal. Isyarat haptik lembut, jangkar visual yang menenangkan, dan lingkar pendamping terenkripsi—tanpa angka detak jantung yang memicu panik.",
    ctaDownload: "Unduh untuk Wear OS & Android",
    ctaExplore: "Coba Simulator Jangkar Sensorik ↓",
    stat1Val: "5–15 mnt",
    stat1Label: "Waktu Dini untuk Regulasi Diri",
    stat2Val: "100%",
    stat2Label: "Data Biometrik Tersimpan Lokal",
    stat3Val: "0",
    stat3Label: "Angka Detak Jantung Panik",
    stat4Val: "Gratis",
    stat4Label: "Sepenuhnya Gratis untuk Semua",
  },

  simulator: {
    badge: "Simulator Jangkar Sensorik",
    title: "Didesain untuk Menenangkan, Bukan Memicu Panik",
    sub: "Jam tangan olahraga biasa menampilkan grafik detak jantung merah dan angka panik yang memperparah stres. BioCue menggantinya dengan tunas alami yang tenang dan transisi warna lembut.",
    calmBtn: "🌱 Keseimbangan Tenang",
    risingBtn: "✨ Stimulasi Meningkat",
    alertBtn: "🌩️ Sensory Overload",
    activeBtn: "🏃 Aktivitas Fisik Sehat",
    watchViewTitle: "Layar Smartwatch Wear OS (Jangkar Ketenangan)",
    phoneViewTitle: "Aplikasi Pendamping Android (Tampilan Pengguna & Pendamping)",
  },

  howItWorks: {
    badge: "Siklus Perlindungan Tiga Tahap",
    title: "Bagaimana BioCue Menjaga Keseimbangan Anda",
    sub: "Mulai dari pemantauan sensor latar belakang yang hening hingga intervensi dini yang lembut, BioCue menghubungkan respons fisiologis dengan langkah regulasi diri yang nyata.",
    step1Title: "1. Pemantauan Sensor Hening",
    step1Desc: "Smartwatch Wear OS Anda mengukur interval denyut fotopletismografi (PPG) dan data akselerometer 3-sumbu, memantau Heart Rate Variability (HRV RMSSD) dan gerak fisik tanpa menguras baterai.",
    step2Title: "2. Prediksi Mandiri di Perangkat",
    step2Desc: "Algoritma Fusi ML kami menganalisis tonus otonom secara lokal di basis data Room ponsel Anda. Membedakan antara olahraga sehat dan stres sensorik—data mentah tidak pernah meninggalkan perangkat.",
    step3Title: "3. Isyarat Lembut & Bantuan",
    step3Desc: "Sebelum stres memuncak menjadi krisis, jam tangan memancarkan getaran haptik lembut. Jika ketegangan meningkat, pengguna dapat menyalakan panduan napas atau mengirim sinyal SOS 1-ketuk ke hingga 2 pendamping resmi.",
  },

  features: {
    badge: "Dirancang untuk Individu Autistik & Sensitif Sensorik",
    title: "Rekayasa Penuh Perhatian untuk Kesejahteraan Sensorik",
    sub: "Setiap interaksi, animasi, dan warna mematuhi Sistem Bahasa Desain kami yang ramah neurodivergen.",
  },

  faq: {
    badge: "Pertanyaan Umum",
    title: "Pertanyaan yang Sering Diajukan",
    sub: "Jawaban transparan seputar sensor, batasan privasi, semangat open-source, dan pemakaian sehari-hari.",
    items: [
      {
        q: "Apakah BioCue benar-benar 100% gratis?",
        a: "Ya. BioCue sepenuhnya gratis, non-komersial, dan open-source. Kami meyakini bahwa teknologi bantu yang melindungi kemandirian dan martabat individu neurodivergen tidak boleh dibatasi oleh biaya langganan.",
      },
      {
        q: "Bagaimana BioCue memprediksi sensory overload sebelum terjadi?",
        a: "Sebelum meltdown atau shutdown sensorik tampak dari luar, sistem saraf simpatis mulai aktif, memicu penurunan halus pada Heart Rate Variability (HRV RMSSD) disertai peningkatan detak jantung saat tubuh tidak aktif bergerak. Algoritma Fusi BioCue mengenali pola ini 5–15 menit lebih awal.",
      },
      {
        q: "Mengapa jam tangan tidak menampilkan angka detak jantung?",
        a: "Melihat angka seperti '148 BPM' sering kali memicu kepanikan dan siklus kecemasan baru. BioCue menggunakan isyarat warna (Hijau Tenang, Amber Waspada, Koral Pendamping) dan animasi pernapasan ritmis untuk menjaga ketenangan pengguna.",
      },
      {
        q: "Perangkat apa saja yang saya butuhkan?",
        a: "Anda membutuhkan smartwatch Wear OS 3.0+ dengan sensor detak jantung optik standar (seperti Samsung Galaxy Watch 4/5/6/7, Google Pixel Watch 1/2/3, atau Mobvoi TicWatch Pro 5) dan smartphone atau tablet Android dengan Android 8.0 ke atas.",
      },
      {
        q: "Bisakah pendamping melacak lokasi saya terus-menerus?",
        a: "Tidak. Pelacakan GPS terus-menerus dilarang keras dalam arsitektur perangkat lunak kami. Lokasi GPS hanya diambil saat Anda secara sengaja menekan tombol SOS atau mengonfirmasi status krisis akut.",
      },
      {
        q: "Bagaimana cara mencabut izin pendamping yang terhubung?",
        a: "Buka aplikasi BioCue Companion di ponsel Anda, masuk ke Lingkar Pendamping, lalu ketuk 'Hapus Pendamping'. Server seketika memutuskan sambungan, dan ponsel pendamping langsung menghapus semua riwayat lokal.",
      },
    ],
  },

  download: {
    badge: "Tersedia Sekarang",
    title: "Pasang BioCue di Smartwatch & Ponsel Anda",
    sub: "Mulai dalam hitungan menit dengan aplikasi mandiri Wear OS dan klien pendamping Android.",
    wearTitle: "Aplikasi Jam Wear OS",
    wearSub: "Tampilan jam bebas cemas, Tile Wear OS akses cepat, dan pemantauan sensor PPG hening.",
    wearCta: "Unduh APK Wear OS / Play Store",
    mobileTitle: "Aplikasi Pendamping Android",
    mobileSub: "Skor basis data Room lokal, jurnal sensorik, dan pengelolaan Lingkar Pendamping 2-in-1.",
    mobileCta: "Unduh Pendamping Android",
    githubTitle: "Repositori Open-Source",
    githubSub: "Telusuri kode sumber lengkap, periksa aturan keamanan, atau kompilasi rilis sendiri.",
    githubCta: "Lihat di GitHub ↗",
  },
};
