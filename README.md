# SAKUS Plus (Sakarya Ulaşım Sistemi)

SAKUS Plus, Sakarya Büyükşehir Belediyesi'nin toplu taşıma araçları için geliştirilmiş alternatif ve açık kaynaklı bir ulaşım takip uygulamasıdır. Jetpack Compose ile modern bir arayüze ve akıcı bir kullanıcı deneyimine sahip olacak şekilde tasarlanmıştır. Uygulama içerisinde güncel haberler, duyurular, sefer saatleri, canlı araç takibi, duraklar ve hat güzergahları hakkındaki bilgilere anlık olarak erişilebilir.

## 🚀 Özellikler
- **Canlı Araç Takibi:** Seçilen hattaki araçların konumlarını harita üzerinde canlı olarak izleme.
- **Hatlar ve Duraklar:** Belediye otobüsleri, Özel Halk otobüsleri, Minibüsler ve diğer araç türleri dahil tüm taşıma ağını görüntüleme.
- **Sefer Saatleri:** Seçilen hattın güncel sefer saatlerini listeleme.
- **Haberler ve Duyurular:** Sakarya Büyükşehir Belediyesi'ne ait en güncel duyuru ve ulaşım haberlerine anında erişim.
- **Modern Arayüz (UI):** Jetpack Compose mimarisi sayesinde hızlı, pürüzsüz ve karanlık/aydınlık tema destekli.

---

## 📡 API Dokümantasyonu

Uygulamanın tam kapasiteyle çalışmasını sağlayan REST ve SSE (Server-Sent Events) tabanlı API uç noktalarının detayları aşağıda verilmiştir. Tüm veriler `SbbApiServisi.kt` sınıfı üzerinden yönetilmektedir.

### 🌐 Temel URL'ler (Base URLs)
- **Toplu Taşıma API:** `https://sbbpublicapi.sakarya.bel.tr/api/v1`
- **Haberler API:** `https://api.sakarya.bel.tr/Mobil/News`

---

### 1. Hatlar (Lines & Routes)
Farklı türdeki toplu taşıma araç hatlarını listeler.
- **Endpoint:** `GET /Ulasim?busType={busType}`
- **Parametreler:**
  - `busType` (Int): Taşıt Türü ID'si
    - `3869` : Belediye Otobüsleri
    - `5731` : Özel Halk Otobüsleri
    - `5733` : Taksi Dolmuş
    - `5732` : Minibüs
    - `6904` : Metrobüs
    - `6905` : Adaray

### 2. Hat Sefer Saatleri (Line Schedules)
Belirli bir hatta yapılması planlanan sefer saatlerini güne göre getirir.
- **Endpoint:** `GET /Ulasim/line-schedule?date={encodedDate}&lineId={lineId}`
- **Parametreler:**
  - `date` (String): İlgili gün (ISO Format: `YYYY-MM-DDThh:mm:ss.msZ`)
  - `lineId` (Int): İlgili hattın benzersiz ID'si.

### 3. Haberler (News)
Toplu taşıma kategorisindeki genel ulaşım haberlerini getirir.
- **Endpoint:** `GET https://api.sakarya.bel.tr/Mobil/News/GetListNews?CategoryId={categoryId}&NewsCount={count}`
- **Parametreler:**
  - `CategoryId` (Int): `52` (Toplu Taşıma haberleri için)
  - `NewsCount` (Int): Getirilecek haber limiti (Örn: `50`)

### 4. Duyurular (Announcements)
Genel ve hat bazlı olmak üzere iki farklı tür duyuru çekilebilmektedir.
- **Hattan Bağımsız Genel Duyurular:** 
  `GET /Ulasim/announcement?isLineAnnouncement=false&pageSize={pageSize}`
- **Tüm Taşıma Ağı Duyuruları:** 
  `GET /Ulasim/announcement?isLineAnnouncement=true&pageSize={pageSize}`
- **Belirli Hattın Duyuruları:** 
  `GET /Ulasim/announcement?isLineAnnouncement=true&lineId={lineId}&pageSize={pageSize}`

### 5. Fiyat Tarifesi (Line Fares)
İlgili seçilen hatta ait biniş ücretlerini listeler.
- **Endpoint:** `GET /Ulasim/line-fare/{lineId}?busType={busType}`
- **Parametreler:**
  - `lineId` (Int): Hat ID'si
  - `busType` (Int): Araç Türü (Varsayılan: `3869`)

### 6. Duraklar ve Güzergahlar (Stops & Routes details)
- **Tüm Durakların Listesi (Sayfalı):**
  `GET /Ulasim/bus-stops-pagination?pageNumber={pageNumber}&pageSize={pageSize}`
- **Belirli Hattın Güzergahı ve İçerdiği Duraklar:**
  `GET /Ulasim/route-and-busstops/{lineId}?date={dateStr}` (Örn DateStr: `YYYY-MM-DD`)

### 7. Canlı Araç Takibi (Live Vehicle Tracking)
Araçların mevcut konumunu ve durağa olan süreçlerini (ETA) getirir. İki farklı yöntem kullanılır:

#### A - Statik Araç Konumları (Long Polling/Manuel Yenileme)
- **Endpoint:** `GET /VehicleTracking?AsisId={asisId}`
- **Parametreler:**
  - `AsisId` (Int): ASIS entegrasyon sistemi ID'si (`asis_map.json` kullanılarak LineID üzerinden parse edilir.)

#### B - Canlı Veri Akışı (Server-Sent Events - SSE Stream)
Haritada sürekli güncellenen ve ping atmadan stream sağlanan verilerdir. Yaklaşık 1-2 saniyede bir yeni konum verisi "event" olarak fırlatılır.
- **Endpoint:** `GET /sakus/vehicle-tracking/stream`
- **Headers:** 
  - `Accept: text/event-stream`
- **Not:** Gelen veri `data:` prefixi altından okunarak JSON şeklinde parse edilmekte ve uygulamanın harita bileşeninde (Marker/Overlay) gösterilmektedir.

---

## 🛠️ Kurulum (Installation)
1. Projeyi bilgisayarınıza klonlayın:
```bash
git clone https://github.com/beratr061/SAKUS-Plus.git
```
2. Android Studio'da (Arctic Fox veya daha yeni) açın.
3. Gradle Sync işleminin tamamlanmasını bekleyin.
4. Bir Emülatör veya gerçek bir Android (Minimum API 26+) cihaza çalıştırın.

---

## 📜 Lisans (License)
Bu proje eğitim ve Topluma hizmet geliştirme amacı taşımaktadır. Sakarya Büyükşehir Belediyesi marka ihlali taşımaz, public APILer kullanılarak entegre edilmiştir.
