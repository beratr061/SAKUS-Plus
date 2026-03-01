<div align="center">

# 🚍 SAKUS Plus

### Sakarya Akıllı Ulaşım Sistemi

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-Educational-blue)](#-lisans)

<br/>

**SAKUS Plus**, Sakarya Büyükşehir Belediyesi toplu taşıma ağı için geliştirilmiş,  
açık kaynaklı ve modern bir ulaşım takip uygulamasıdır.

Canlı araç takibi • Sefer saatleri • Durak bilgileri • Hat güzergahları • Haberler & Duyurular

</div>

---

## 📋 İçindekiler

- [🎯 Genel Bakış](#-genel-bakış)
- [✨ Özellikler](#-özellikler)
- [🏗️ Mimari & Teknoloji Yığını](#️-mimari--teknoloji-yığını)
- [📡 API Dokümantasyonu](#-api-dokümantasyonu)
  - [Temel URL'ler](#-temel-urller)
  - [Hatlar API](#1--hatlar-api)
  - [Sefer Saatleri API](#2--sefer-saatleri-api)
  - [Haberler API](#3--haberler-api)
  - [Duyurular API](#4--duyurular-api)
  - [Fiyat Tarifesi API](#5--fiyat-tarifesi-api)
  - [Duraklar & Güzergahlar API](#6--duraklar--güzergahlar-api)
  - [Canlı Araç Takibi API](#7--canlı-araç-takibi-api)
  - [Durağa Yaklaşan Otobüsler](#8--durağa-yaklaşan-otobüsler)
- [📦 Veri Modelleri](#-veri-modelleri)
- [🗂️ Proje Yapısı](#️-proje-yapısı)
- [🛠️ Kurulum](#️-kurulum)
- [📜 Lisans](#-lisans)

---

## 🎯 Genel Bakış

SAKUS Plus, Sakarya Büyükşehir Belediyesi'nin sunduğu public API'leri kullanarak şehirdeki tüm toplu taşıma araçlarını tek bir uygulama üzerinden takip etmenizi sağlar. **Jetpack Compose** ile tamamen modern declarative UI yaklaşımıyla geliştirilmiş olup, **Material 3** tasarım prensiplerini benimser.

Uygulama **REST API** ve **SSE (Server-Sent Events)** protokolü olmak üzere iki farklı veri iletişim modeli kullanır. REST API'ler durak, hat, duyuru ve haber gibi statik veriler için kullanılırken; SSE stream'i canlı araç konum takibi için saniyede bir güncelleme sağlar.

> **Not:** Bu proje eğitim ve topluma hizmet amacı ile geliştirilmiştir. Sakarya Büyükşehir Belediyesi'nin resmi uygulaması değildir.

---

## ✨ Özellikler

| Özellik | Açıklama |
|---------|----------|
| 🗺️ **Canlı Araç Takibi** | SSE stream ile saniye saniye güncellenen araç konumları, Google Maps üzerinde animasyonlu marker'lar |
| 🚏 **Durak Bilgileri** | 2500+ durak noktası, kümeleme (clustering) ile harita üzerinde görüntüleme, durağa yaklaşan araç bilgisi |
| 🛤️ **Hat Güzergahları** | Gidiş/dönüş yön bilgileri, polyline ile harita üzerinde güzergah çizimi, yön okları |
| ⏰ **Sefer Saatleri** | Hafta içi, cumartesi ve pazar günlerine göre ayrılmış sefer takvimi |
| 💰 **Fiyat Tarifesi** | Tam, öğrenci, 60+ ve ücretsiz gibi tüm kart tiplerine göre biniş ücretleri |
| 📰 **Haberler** | Sakarya Büyükşehir Belediyesi toplu taşıma haberlerinin anlık takibi |
| 📢 **Duyurular** | Genel ve hata özel duyurular, iptal/güzergah değişikliği bildirimleri |
| 🌙 **Karanlık / Aydınlık Tema** | Kullanıcı tercihine veya sistem ayarına göre otomatik tema geçişi |
| 🔍 **Kayıp Eşya** | Toplu taşıma araçlarında unutulan eşya bildirimi |
| ❓ **SSS** | Sıkça Sorulan Sorular bölümü |
| ⚙️ **Ayarlar** | Tema tercihleri ve uygulama konfigürasyonu |

---

## 🏗️ Mimari & Teknoloji Yığını

```
┌──────────────────────────────────────────┐
│              UI Layer (Compose)           │
│  ┌─────────┐ ┌──────────┐ ┌───────────┐  │
│  │ Screens │ │ Dialogs  │ │Components │  │
│  └────┬────┘ └────┬─────┘ └─────┬─────┘  │
│       └───────────┼─────────────┘        │
│              ┌────▼────┐                 │
│              │ViewModels│                │
│              └────┬────┘                 │
├───────────────────┼──────────────────────┤
│           Data Layer                     │
│  ┌────────────┐ ┌─┴───────────┐          │
│  │SbbApiServisi│ │ Repository  │          │
│  │ (REST+SSE) │ │  (Cache)    │          │
│  └──────┬─────┘ └──────┬──────┘          │
│         │         ┌────▼────┐            │
│         │         │Room DB  │            │
│         │         │(SQLite) │            │
│         ▼         └─────────┘            │
│  ┌──────────────┐                        │
│  │  OkHttp      │                        │
│  │  Client      │                        │
│  └──────────────┘                        │
└──────────────────────────────────────────┘
```

### Kullanılan Teknolojiler

| Kategori | Teknoloji | Açıklama |
|----------|-----------|----------|
| **UI Framework** | Jetpack Compose | Declarative UI toolkit |
| **Tasarım** | Material Design 3 | Modern, temiz ve erişilebilir tasarım |
| **Navigasyon** | Navigation Compose | Ekranlar arası tip-güvenli geçişler |
| **Harita** | Google Maps Compose | Maps SDK Compose wrapper'ı |
| **Konum** | Play Services Location | Kullanıcı konum erişimi |
| **HTTP İstemci** | OkHttp 4 | REST ve SSE bağlantıları |
| **JSON Ayrıştırma** | Gson | JSON ↔ Kotlin model dönüşümleri |
| **Veritabanı** | Room (SQLite) | Durak/hat verisi yerel önbellekleme |
| **Arka Plan İşleri** | WorkManager | Periyodik veri senkronizasyonu |
| **Resim Yükleme** | Coil 3 | Haber görselleri, SVG desteği |
| **Dil** | Kotlin 2.0+ | Coroutines, Flow, StateFlow |
| **Min SDK** | API 26 (Android 8.0) | Oreo ve üzeri |
| **Target SDK** | API 36 | En güncel Android API |

---

## 📡 API Dokümantasyonu

Uygulama, Sakarya Büyükşehir Belediyesi'nin public API'lerini kullanır. Tüm API çağrıları `SbbApiServisi.kt` singleton sınıfı üzerinden yönetilir.

### 🌐 Temel URL'ler

| Servis | Base URL | Protokol |
|--------|----------|----------|
| **Toplu Taşıma API** | `https://sbbpublicapi.sakarya.bel.tr/api/v1` | REST (JSON) |
| **Canlı Araç Stream** | `https://sbbpublicapi.sakarya.bel.tr/api/v1/sakus/vehicle-tracking/stream` | SSE |
| **Haberler API** | `https://api.sakarya.bel.tr/Mobil/News` | REST (JSON) |

### HTTP İstemci Yapılandırması

İsteklerde kullanılan başlık set'i:

```http
Origin: https://ulasim.sakarya.bel.tr
Referer: https://ulasim.sakarya.bel.tr
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Accept: application/json
```

SSE stream için ek başlık:

```http
Accept: text/event-stream
```

OkHttp istemci yapılandırması:

| Parametre | REST API | SSE Stream |
|-----------|----------|------------|
| Connect Timeout | 15 saniye | 15 saniye |
| Read Timeout | 15 saniye | ♾️ Sonsuz |
| Proxy | NO_PROXY | NO_PROXY |
| Retry Mekanizması | 3 deneme, exponential backoff | Otomatik yeniden bağlanma |

---

### 1. 🚌 Hatlar API

Farklı türdeki toplu taşıma araç hatlarını listeler. Tüm hatlar paralel olarak (`async/awaitAll`) çekilir ve hat numarasına göre sıralanır.

**Endpoint:**

```http
GET /Ulasim?busType={busType}
```

**Parametreler:**

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `busType` | `Int` | ✅ | Araç türü ID'si |

**Araç Türü ID'leri:**

| ID | Araç Türü | Sabit Adı |
|----|-----------|-----------|
| `3869` | 🚌 Belediye Otobüsleri | `BUS_TYPE_BELEDIYE` |
| `5731` | 🚍 Özel Halk Otobüsleri | `BUS_TYPE_OZEL_HALK` |
| `5733` | 🚐 Taksi Dolmuş | `BUS_TYPE_TAKSI_DOLMUS` |
| `5732` | 🚐 Minibüs | `BUS_TYPE_MINIBUS` |
| `6904` | 🚎 Metrobüs | `BUS_TYPE_METROBUS` |
| `6905` | 🚈 Adaray | `BUS_TYPE_ADARAY` |

**Yanıt Modeli:** `List<HatBilgisi>`

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | `Int` | Hat benzersiz ID'si |
| `ad` | `String` | Hat adı (örn: "ADAPAZARI - ERENLER") |
| `hatNumarasi` | `String` | Görüntülenen hat numarası (örn: "3") |
| `aracTipAdi` | `String` | Araç türü adı |
| `aracTipId` | `Int` | Araç türü ID referansı |
| `asisId` | `Int?` | ASIS entegrasyon ID'si (nullable) |

**Örnek İstek:**

```bash
curl -H "Origin: https://ulasim.sakarya.bel.tr" \
     "https://sbbpublicapi.sakarya.bel.tr/api/v1/Ulasim?busType=3869"
```

**Not:** Uygulama tüm 6 araç türünü paralel olarak çeker, birleştirir, `distinctBy { id }` ile tekrarları kaldırır ve hat numarasına göre doğal sıralama uygular.

---

### 2. ⏰ Sefer Saatleri API

Belirli bir hatta yapılması planlanan sefer saatlerini güne göre getirir. Hafta içi, cumartesi ve pazar günleri için ayrı ayrı sorgulanır.

**Endpoint:**

```http
GET /Ulasim/line-schedule?date={encodedDate}&lineId={lineId}
```

**Parametreler:**

| Parametre | Tip | Zorunlu | Format | Açıklama |
|-----------|-----|---------|--------|----------|
| `date` | `String` | ✅ | `YYYY-MM-DDT00:00:00.000Z` (URL encoded) | Sorgulanacak tarih |
| `lineId` | `Int` | ✅ | - | Hat ID'si |

**Yanıt Modeli:** `HatSeferBilgisi`

```
HatSeferBilgisi
├── seferler: List<GuzergahSefer>
│   ├── guzergahAdi: String        // "ADAPAZARI - ERENLER"
│   ├── yon: Int                    // 0 = Gidiş, 1 = Dönüş
│   └── detaylar: List<SeferDetay>
│       ├── baslangicSaat: String   // "06:30"
│       └── bitisSaat: String       // "07:15"
```

**Kullanım Akışı:**

Uygulama, seçilen hat için 3 farklı gün tipi sorgusu yapar:
1. **Hafta içi:** En yakın Pazartesi günü
2. **Cumartesi:** En yakın Cumartesi günü
3. **Pazar:** En yakın Pazar günü

Her sorgu için `Calendar` nesnesi oluşturulup tarih ISO formatına çevrilerek URL encode edilir.

---

### 3. 📰 Haberler API

Sakarya Büyükşehir Belediyesi'nin toplu taşıma kategorisindeki haberlerini getirir. Bu endpoint farklı bir base URL kullanır.

**Endpoint:**

```http
GET https://api.sakarya.bel.tr/Mobil/News/GetListNews?CategoryId={categoryId}&NewsCount={count}
```

**Parametreler:**

| Parametre | Tip | Varsayılan | Açıklama |
|-----------|-----|------------|----------|
| `CategoryId` | `Int` | `52` | Toplu Taşıma haberleri kategori ID'si |
| `NewsCount` | `Int` | `50` | Getirilecek maksimum haber sayısı |

**Yanıt Modeli:** `List<NewsItem>`

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | `Int` | Haber ID'si |
| `title` | `String` | Haber başlığı |
| `summary` | `String` | Haber özeti |
| `content` | `String` | HTML içerik |
| `imageUrl` | `String` | Kapak fotoğrafı URL'i |
| `date` | `String` | Yayın tarihi |

**Not:** Haber görselleri `Coil 3` kütüphanesi ile asenkron olarak yüklenir ve `AsyncImage` composable'ı ile gösterilir.

---

### 4. 📢 Duyurular API

Üç farklı duyuru sorgu modu desteklenir:

#### 4a. Genel Duyurular (Hattan Bağımsız)

```http
GET /Ulasim/announcement?isLineAnnouncement=false&pageSize={pageSize}
```

#### 4b. Tüm Hat Duyuruları

```http
GET /Ulasim/announcement?isLineAnnouncement=true&pageSize={pageSize}
```

#### 4c. Belirli Bir Hattın Duyuruları

```http
GET /Ulasim/announcement?isLineAnnouncement=true&lineId={lineId}&pageSize={pageSize}
```

**Parametreler:**

| Parametre | Tip | Varsayılan | Açıklama |
|-----------|-----|------------|----------|
| `isLineAnnouncement` | `Boolean` | - | `false`: genel, `true`: hat bazlı |
| `lineId` | `Int` | - | Hat ID (sadece 4c için) |
| `pageSize` | `Int` | `100` | Sayfa boyutu |

**Yanıt Modeli:** `List<Duyuru>`

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | `Int` | Duyuru ID'si |
| `baslik` | `String` | Duyuru başlığı |
| `icerik` | `String` | HTML formatında içerik |
| `duzMetin` | `String` | HTML taglerinden arındırılmış düz metin |
| `hatId` | `Int` | İlgili hat ID'si |
| `tarih` | `String` | Yayın tarihi |

**Filtreleme:** Hat bazlı duyurular API yanıtı sonrasında `hatId` ile filtrelenir: `all.filter { it.hatId == lineId }`

---

### 5. 💰 Fiyat Tarifesi API

Seçilen hattın biniş ücretlerini kart tiplerine göre getirir.

**Endpoint:**

```http
GET /Ulasim/line-fare/{lineId}?busType={busType}
```

**Parametreler:**

| Parametre | Tip | Varsayılan | Açıklama |
|-----------|-----|------------|----------|
| `lineId` | `Int` | - | Hat ID'si (path variable) |
| `busType` | `Int` | `3869` | Araç türü ID'si |

**Yanıt Modeli:** `TarifeBilgisi`

```
TarifeBilgisi
├── tarifeTipleri: List<TarifeTipi>
│   ├── id: Int                     // Kart tipi ID
│   └── tipAdi: String             // "Tam", "Öğrenci", "60+", "Ücretsiz"
└── gruplar: List<TarifeGrubu>
    ├── ad: String                  // Tarife grubu adı
    └── guzergahlar: List<TarifeGuzergah>
        ├── guzergahAdi: String     // "ADAPAZARI - ERENLER"
        └── ucretler: List<TarifeUcret>
            ├── tarifeTipId: Int    // Kart tipi ID referansı
            └── sonUcret: String    // "15.50"
```

---

### 6. 🚏 Duraklar & Güzergahlar API

#### 6a. Tüm Duraklar (Sayfalı)

Tüm durak noktalarını sayfalı olarak getirir. Uygulama tüm sayfaları otomatik olarak traversal eder.

```http
GET /Ulasim/bus-stops-pagination?pageNumber={page}&pageSize={size}
```

| Parametre | Tip | Varsayılan | Açıklama |
|-----------|-----|------------|----------|
| `pageNumber` | `Int` | `1` | Sayfa numarası (1'den başlar) |
| `pageSize` | `Int` | `100` | Sayfa başına durak sayısı |

**Otomatik Sayfalama Algoritması:**

```kotlin
while (true) {
    val pageStops = duraklariGetirSayfa(pageNumber, pageSize)
    if (pageStops.isEmpty()) break           // Veri kalmadı
    allStops.addAll(pageStops)
    if (pageStops.size < pageSize) break      // Son sayfa
    pageNumber++
}
```

**Yanıt Modeli:** `List<DurakBilgisi>`

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | `Int` | Durak ID'si |
| `durakAdi` | `String` | Durak adı |
| `lat` | `Double` | Enlem koordinatı |
| `lng` | `Double` | Boylam koordinatı |
| `yon` | `String` | Yön bilgisi |

**Not:** Koordinatı `(0.0, 0.0)` olan duraklar filtrelerden kaldırılır.

#### 6b. Hat Güzergahı & İçerdiği Duraklar

Belirli bir hattın güzergah polyline'ını ve sıralı durak listesini getirir.

```http
GET /Ulasim/route-and-busstops/{lineId}?date={dateStr}
```

| Parametre | Tip | Format | Açıklama |
|-----------|-----|--------|----------|
| `lineId` | `Int` | - | Hat ID'si |
| `date` | `String` | `YYYY-MM-DD` | Sorgu tarihi |

**Yanıt Modeli:** `HatGuzergahBilgisi`

```
HatGuzergahBilgisi
└── yonler: List<YonBilgisi>
    ├── yon: Int                    // 0 = Gidiş, 1 = Dönüş
    ├── guzergahWkt: String         // WKT polyline (LINESTRING format)
    └── duraklar: List<DurakBilgisi>
```

---

### 7. 🔴 Canlı Araç Takibi API

Canlı araç takibi iki farklı yöntemle sağlanır:

#### 7a. Statik Araç Konumları (REST — Poll)

Belirli bir ASIS ID'si üzerinden araçların anlık konumunu getirir.

```http
GET /VehicleTracking?AsisId={asisId}
```

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `AsisId` | `Int` | ASIS entegrasyon sistemi ID'si |

**ASIS ID Çözümleme:**

Bir hattın ASIS ID'sini bulmak için uygulama şu adımları izler:
1. `HatBilgisi.asisId` alanı doğrudan varsa kullanılır
2. Yoksa `asis_map.json` dosyasından `sbb_id → asis_id` eşlemesi yapılır
3. Hâlâ bulunamazsa hat numarası (`line_no`) ile eşleme denenir

**Retry Mekanizması:**

REST endpoint'i için 3 denemelik exponential backoff mekanizması uygulanır:

```kotlin
for (attempt in 1..3) {
    val result = get(url)
    if (result != null) return result
    delay(500L * attempt)  // 500ms, 1000ms, 1500ms
}
```

#### 7b. Canlı Veri Akışı (SSE — Server-Sent Events)

> 🔴 **Gerçek zamanlı** — Yaklaşık **1-2 saniyede bir** yeni konum verisi alınır.

```http
GET /sakus/vehicle-tracking/stream

Headers:
  Accept: text/event-stream
```

**SSE Protokolü Detayları:**

SSE stream'i standart event-stream formatında veri gönderir:

```
event: vehicle-update
data: [{"lineId":50,"busNumber":3,"latitude":40.73,"longitude":30.39,...}]

event: server-disconnect
data: "Bağlantı süresi doldu"
```

**Event Tipleri:**

| Event | Açıklama | Uygulama Davranışı |
|-------|----------|-------------------|
| `vehicle-update` | Araç konum güncellemesi | JSON parse → lineId filtreleme → UI emit |
| `server-disconnect` | Sunucu 10 dk sonra bağlantıyı kapatır | Otomatik yeniden bağlanma |

**Akış Diyagramı:**

```
┌─────────┐     SSE Stream      ┌─────────────┐
│  Sunucu  │ ─────────────────► │ OkHttp      │
│  (SBB)   │  event: data:...   │ StreamClient│
└─────────┘                     └──────┬──────┘
                                       │
                                  BufferedReader
                                  readLine()
                                       │
                                ┌──────▼──────┐
                                │ JSON Parse  │
                                │ (Gson)      │
                                └──────┬──────┘
                                       │
                                  lineId Filter
                                       │
                                ┌──────▼──────┐
                                │ Kotlin Flow │
                                │ emit()      │
                                └──────┬──────┘
                                       │
                                  collectAsState
                                       │
                                ┌──────▼──────┐
                                │  Google Map │
                                │  Markers    │
                                └─────────────┘
```

**Yanıt Modeli:** `List<AracKonumu>`

| Alan | Tip | Açıklama |
|------|-----|----------|
| `plaka` | `String` | Araç plakası |
| `aracNumarasi` | `Int` | Otobüs kapı numarası |
| `enlem` | `Double` | Latitude |
| `boylam` | `Double` | Longitude |
| `hiz` | `Double` | Anlık hız (km/h) |
| `yon` | `Double` | Hareket yönü (derece, 0-360) |
| `hatNo` | `String` | Hat numarası |
| `guzergahAdi` | `String` | Güzergah adı |
| `mevcutDurak` | `String` | Şu an bulunduğu durak |
| `sonrakiDurak` | `String` | Sonraki durak |
| `etaSaniye` | `Long` | Tahmini varış süresi (saniye) |
| `aktifMi` | `Boolean` | Araç aktif durumda mı |
| `hizFormati` | `String` | Formatlanmış hız (örn: "45 km/h") |

**Hata Yönetimi & Yeniden Bağlanma:**

```
Stream Kopması → 2 sn bekleme → Yeniden bağlanma → Sonsuz döngü
Server Disconnect Event → Hemen yeniden bağlanma (bekleme yok)
Parse Hatası → Log & devam (stream kesilmez)
```

---

### 8. 🚏 Durağa Yaklaşan Otobüsler

Bu endpoint doğrudan bir API çağrısı değildir — birden fazla API çağrısının birleştirilmesiyle oluşturulan **türetilmiş bir veri kaynağıdır**.

**Algoritma:**

```
1. Parametre olarak gelen durak adını normalize et
2. Sistemdeki ilk 30 hat için döngüye gir
3. Her hat için ASIS ID'yi çözümle
4. Her hat için araç konumlarını çek (REST)
5. Her aktif aracın sonraki/mevcut durağını kontrol et
6. Durak adı eşleşmesi varsa → DurakVarisi listesine ekle
7. Tahmini dakikaya göre sırala ve döndür
```

**Yanıt Modeli:** `List<DurakVarisi>`

| Alan | Tip | Açıklama |
|------|-----|----------|
| `hatNo` | `String` | Hat numarası |
| `hatAdi` | `String` | Hat adı |
| `plaka` | `String` | Araç plakası |
| `aracNumarasi` | `Int` | Araç numarası |
| `dakika` | `Int` | Tahmini varış (dk), 0 = durakta |
| `guzergahAdi` | `String` | Güzergah adı |

---

## 📦 Veri Modelleri

Tüm veri modelleri `com.berat.sakus.data.models` paketinde organize edilmiştir:

| Dosya | Modeller | Açıklama |
|-------|----------|----------|
| `HatBilgisi.kt` | `HatBilgisi` | Hat bilgileri |
| `NewsItem.kt` | `NewsItem` | Haber verisi |
| `Duyuru.kt` | `Duyuru` | Duyuru verisi |
| `SeferModels.kt` | `HatSeferBilgisi`, `GuzergahSefer`, `SeferDetay` | Sefer zamanlama |
| `TarifeModels.kt` | `TarifeBilgisi`, `TarifeTipi`, `TarifeGrubu`, `TarifeGuzergah`, `TarifeUcret` | Fiyatlandırma |
| `AracKonumu.kt` | `AracKonumu` | Canlı araç konum verisi |
| `GuzergahModels.kt` | `YonBilgisi`, `DurakBilgisi`, `HatGuzergahBilgisi` | Güzergah & durak |

Her model, API JSON yanıtından dönüşüm için `fromJson(JsonObject)` companion factory methoduna sahiptir.

---

## 🗂️ Proje Yapısı

```
app/src/main/java/com/berat/sakus/
├── MainActivity.kt                    # Ana giriş noktası, navigasyon grafiği
├── data/
│   ├── SbbApiServisi.kt              # Tüm API çağrıları (Singleton)
│   ├── ModelAliases.kt               # Geriye uyumluluk type alias'ları
│   ├── models/                       # Veri modelleri
│   │   ├── HatBilgisi.kt
│   │   ├── NewsItem.kt
│   │   ├── Duyuru.kt
│   │   ├── SeferModels.kt
│   │   ├── TarifeModels.kt
│   │   ├── AracKonumu.kt
│   │   └── GuzergahModels.kt
│   ├── local/                        # Room veritabanı
│   │   ├── SakusDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   ├── repository/
│   │   └── TransportRepository.kt    # Veri kaynağı soyutlaması
│   └── sync/
│       └── SyncManager.kt            # WorkManager periyodik sync
├── ui/
│   ├── theme/
│   │   ├── Color.kt                  # Renk tanımları
│   │   ├── Theme.kt                  # Material 3 tema
│   │   └── ThemeManager.kt           # Tema tercihi yönetimi
│   └── screens/
│       ├── SplashScreen.kt           # Açılış ekranı
│       ├── SakusHomeScreen.kt        # Ana sayfa + Drawer
│       ├── TransportationScreen.kt   # Hat listesi
│       ├── LineMapScreen.kt          # Canlı harita takibi
│       ├── LineMapDialogs.kt         # Sefer/tarife dialog'ları
│       ├── MapHelpers.kt             # Harita araçları
│       ├── DuraklarScreen.kt         # Durak haritası
│       ├── DurakDetailScreen.kt      # Durak detayı
│       ├── HatSeferSaatleriScreen.kt # Tüm hatlar sefer saati
│       ├── NewsScreen.kt             # Haber listesi
│       ├── NewsDetailScreen.kt       # Haber detayı
│       ├── DuyurularScreen.kt        # Duyuru listesi
│       ├── DuyuruDetailScreen.kt     # Duyuru detayı
│       ├── LostPropertyScreen.kt     # Kayıp eşya
│       ├── AboutScreen.kt            # Hakkında
│       ├── FaqScreen.kt              # SSS
│       ├── SettingsScreen.kt         # Ayarlar
│       └── ComingSoonScreen.kt       # Yakında (placeholder)
└── assets/
    └── json/
        └── asis_map.json             # ASIS ID eşleme tablosu
```

---

## 🛠️ Kurulum

### Gereksinimler

- **Android Studio:** Hedgehog (2023.1) veya üzeri
- **JDK:** 11+
- **Android SDK:** API 26+ (min), API 36 (target)
- **Google Maps API Key:** [Google Cloud Console](https://console.cloud.google.com/) üzerinden `Maps SDK for Android` etkinleştirin

### Adımlar

**1. Projeyi klonlayın:**

```bash
git clone https://github.com/beratr061/SAKUS-Plus.git
cd SAKUS-Plus
```

**2. Google Maps API anahtarınızı ekleyin:**

Proje kök dizinindeki `local.properties` dosyasına ekleyin:

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
```

> ⚠️ `local.properties` dosyası `.gitignore` tarafından takipten hariç tutulmuştur. API anahtarınızı asla commit etmeyin.

**3. Gradle Sync:**

Android Studio projeyi açtığında otomatik olarak sync yapacaktır. Manuel olarak:

```bash
./gradlew assembleDebug
```

**4. Çalıştırın:**

Bir emülatör veya fiziksel Android cihaz (API 26+) üzerinde uygulamayı başlatın.

---

## 📜 Lisans

Bu proje **eğitim ve topluma hizmet** amacıyla geliştirilmiştir. Sakarya Büyükşehir Belediyesi'nin resmi uygulaması değildir. Kullanılan API'ler belediyenin kamuya açık (public) servisleridir.

---

<div align="center">

**Sakarya'da toplu taşıma artık parmaklarınızın ucunda.** 🚍

Made with ❤️ in Sakarya

</div>
