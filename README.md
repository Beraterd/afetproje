# İstanbul Afet Koordinasyon Platformu

**Afet öncesi hazırlık, afet anında bilgi paylaşımı ve saha koordinasyonu için geliştirilen web tabanlı bitirme projesi.** İstanbul'daki olayları, gönüllü ekipleri, hasar kayıtlarını ve kaynak taleplerini harita ve yönetim ekranlarında bir araya getirir.

## Canlı siteyi inceleyin

### [🌐 Canlı Site → afetistanbul.online](https://afetistanbul.online)

**Kurulum yapmadan ve hesap oluşturmadan projeyi gezebilirsiniz.**

1. [Giriş ekranını açın](https://afetistanbul.online/login).
2. **“Giriş Yapmadan Admin Olarak Siteyi Gez”** düğmesini seçin.
3. Kontrol panelinden başlayıp sol menüdeki modülleri inceleyin.

Demo oturumu **salt okunurdur**: veri ekleme, değiştirme ve silme işlemleri devre dışıdır. Konum izni vermeden **“Şimdi Değil”** seçeneğiyle incelemeye devam edebilirsiniz.

> **English:** A full-stack disaster coordination platform for Istanbul, combining operational maps, earthquake feeds, volunteer coordination, damage assessments, resource requests and reporting. [Explore the live site](https://afetistanbul.online) using the read-only admin demo; no account registration is required.

## Hangi problemi ele alır?

Afet sırasında olay bilgisi, ekip ihtiyacı, bina hasarı ve yardım kaynakları farklı kanallarda dağınık kalabilir. Bu proje, farklı kullanıcı rollerinin aynı platformda bu bilgileri takip edebilmesini ve görevleri bölge bazında koordine edebilmesini amaçlar.

| Kullanıcı | Platformdaki rolü |
| --- | --- |
| Yönetici | Genel görünüm, kullanıcı ve koordinatör yönetimi, raporlar |
| İlçe / mahalle koordinatörü | Bölgesel olaylar, ihtiyaçlar, hasar kayıtları ve görevler |
| Gönüllü | Olayları inceleme, görev takibi ve saha bilgisi paylaşımı |

## Özellikler

| Modül | Ne sunar? |
| --- | --- |
| Kontrol paneli | Ekip ihtiyaçları, hasar kayıtları, kaynak talepleri ve son deprem görünümü |
| Operasyon haritası | İlçe, mahalle, sokak ve bina katmanları; konuma bağlı olay ve hasar görünümü |
| Deprem takibi | AFAD ve Kandilli kaynaklarından alınan deprem kayıtları |
| Olay ve görev yönetimi | Ekip ihtiyacının takibi, ekip/gönüllü atamaları ve kişisel görev ekranı |
| Hasar tespiti | Fotoğraflı kayıtlar, saha doğrulaması ve koordinatör onay akışı |
| Kaynak yönetimi | Yardım talepleri ve kaynak stoklarının takibi |
| Afet hazırlığı | Toplanma alanları, acil durum yakınları ve konum bağlantısıyla mesaj paylaşımı |
| Raporlama | Operasyon, risk/hasar ve kaynak raporları; PDF ve Excel dışa aktarımı |
| Organizasyon | Koordinatör atamaları, gönüllü belgeleri ve belge onayları |
| Simülasyon | Afet senaryolarını yönetim ekranları üzerinden inceleme |

E-posta, SMS, WhatsApp ve yapay zekâ destekli analiz için entegrasyonlar bulunur. Bu işlevler ilgili servis yapılandırmasına ve hesap yetkilerine bağlıdır; canlı demo tüm dış servislerin gönderim veya analiz işlevlerini deneme amacı taşımaz.

## Ziyaretçiler için önerilen inceleme sırası

1. **Kontrol Paneli:** Platformun genel görünümünü ve dikkat gerektirenleri inceleyin.
2. **Operasyon → Operasyon Haritası:** Konum temelli görünümü ve bölgesel aramayı deneyin.
3. **Afet & Hazırlık → Depremler / Toplanma Alanları:** Deprem kayıtlarını ve hazırlık bilgilerini görün.
4. **Operasyon → Olaylar / Hasar Tespiti / Kaynak Talepleri:** Saha koordinasyonunun nasıl modellendiğini inceleyin.
5. **Raporlama → Raporlar:** Verilerin yönetim raporlarına dönüşümünü görün.

## Kullanılan teknolojiler

| Katman | Teknolojiler |
| --- | --- |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| Veri ve formlar | TanStack Query, Axios, Zustand, React Hook Form, Zod |
| Harita | Leaflet / React Leaflet, MapLibre GL, OpenStreetMap verileri |
| Backend | Java 17, Spring Boot 3.2.3, Spring Security, Spring Data JPA |
| Veritabanı | PostgreSQL 16, Flyway migration'ları |
| Kimlik doğrulama | JWT, yenileme oturumu ve rol bazlı erişim |
| Çevrimdışı destek | PWA, Service Worker, IndexedDB ve senkronizasyon kuyruğu |
| Rapor çıktıları | Apache POI, OpenPDF |
| Test / otomasyon | JUnit ve Spring testleri, Vitest, Testing Library, GitHub Actions |

## Teknik yaklaşım

```text
React / TypeScript arayüzü
    ├── Harita ve bölgesel veri görünümü
    ├── Formlar, rol bazlı sayfalar ve raporlar
    └── IndexedDB / çevrimdışı işlem kuyruğu
                    ↓ REST API
Spring Boot → Controller → Service → Repository → PostgreSQL
    ├── JWT ve rol bazlı erişim kontrolü
    ├── Deprem verisi toplama ve bildirim servisleri
    └── Dosya, belge ve rapor işlemleri
```

Kaynak kodda çevrimdışı kuyruk/senkronizasyon, salt okunur demo koruması, istek sınırlandırma, dosya doğrulama ve veritabanı migration'ları ayrı bileşenler olarak düzenlenmiştir. Çevrimdışı destek, önceden alınmış veri ve desteklenen işlemlerle sınırlıdır; tüm canlı servislerin internetsiz çalıştığı anlamına gelmez.

## Depo yapısı

```text
afetproje/
├── frontend/          # React / TypeScript uygulaması ve arayüz testleri
├── backend/           # Spring Boot API, migration'lar ve backend testleri
├── docs/              # Teknik tasarım ve kurulum dokümanları
├── scripts/           # İstanbul coğrafi verileri için hazırlık / kontrol araçları
├── .github/workflows/ # Otomatik kontrol tanımları
└── *.pdf / *.pptx     # Proje raporları ve sunum
```

## Yerel geliştirme

Gereksinimler: **JDK 17, Maven, Node.js 20+, npm ve Docker Compose** (veya PostgreSQL 16).

Depoyu klonlayın:

```bash
git clone https://github.com/Beraterd/afetproje.git
cd afetproje
```

### Backend

`backend` klasöründe PostgreSQL'i başlatın:

```bash
cd backend
docker compose up -d
```

[backend/.env.example](backend/.env.example) dosyası kullanılabilecek değişkenleri açıklar. Spring Boot ortam değişkenlerini okur; bu dosyayı kopyalamak tek başına değişkenleri uygulamaya yüklemez. Yerel çalıştırma için örnek PowerShell ayarları:

```powershell
$env:JWT_SECRET='yalnizca-yerel-demo-icin-32-karakterden-uzun-ornek'
$env:EMAIL_TYPE='mock'
$env:AI_ENABLED='false'
mvn spring-boot:run
```

Docker Compose ve geliştirme yapılandırmasındaki varsayılan veritabanı bağlantısı `localhost:5433/afetdb` adresine yönelir. Kendi veritabanınızı kullanıyorsanız `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` ve `SPRING_DATASOURCE_PASSWORD` değişkenlerini ayarlayın. Örnek yerel değerleri canlı ortamda kullanmayın.

### Frontend

Başka bir terminalde depo kökünden:

```powershell
cd frontend
Copy-Item .env.example .env.local
npm ci
npm run dev
```

Arayüz `http://localhost:5173`, API `http://localhost:8080` adresinde çalışır. Demo giriş özelliği açıkken yerel uygulamayı da giriş ekranındaki demo düğmesiyle inceleyebilirsiniz.

### Kontrol komutları

Frontend: `npm run typecheck`, `npm run lint`, `npm run test`, `npm run build`.

Backend: `mvn test`, `mvn package`. Entegrasyon testleri için PostgreSQL gerekir. Bu komutların otomasyon tanımı [.github/workflows/ci.yml](.github/workflows/ci.yml) dosyasındadır; güncel sonuçlar [GitHub Actions](https://github.com/Beraterd/afetproje/actions) sayfasından görülebilir.

## Dokümantasyon

- [Frontend kurulum rehberi](docs/RUN_FRONTEND.md)
- [Teknik kapsam — bölüm 1](docs/master-spec-part1.md)
- [Teknik kapsam — bölüm 2](docs/master-spec-part2.md)
- [Frontend tasarım dokümanı](docs/frontend-spec.md)
- [Canlı ortam yapılandırma rehberi](docs/PRODUCTION_CHECKLIST.md)

Tasarım dokümanları hedeflenen kapsamı da içerir; canlı sitede görünen davranış ile her tasarım maddesinin tamamlandığı varsayılmamalıdır. Bu depo bitirme projesini ve uygulama geliştirme yaklaşımını sunar; resmi bir acil yardım hizmeti değildir.

**[Canlı siteyi gez](https://afetistanbul.online) · [Demo giriş ekranı](https://afetistanbul.online/login) · [GitHub profili](https://github.com/Beraterd)**
