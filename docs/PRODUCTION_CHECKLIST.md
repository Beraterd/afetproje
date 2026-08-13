# Production Deploy Checklist — AFET Koordinasyon Sistemi

Bu doküman, prod'a (afetistanbul.online) her deploy öncesi elle (veya CI'da) gözden
geçirilmesi gereken minimum kontrol listesidir. `backend`'de `prod` profili aktifken kritik
maddelerin bir kısmı **otomatik fail-fast** doğrulanır (bkz. `ProductionReadinessValidator`,
`src/main/java/com/afet/koordinasyon/config/ProductionReadinessValidator.java`) — yanlış
yapılandırılmış bir instance sessizce açılmaz, başlangıçta exception fırlatıp durur.

---

## Security

| Kontrol | Nasıl doğrulanır | Otomatik mi? |
|---|---|---|
| JWT secret varsayılan/kısa değil | `JWT_SECRET` env, ≥32 karakter, `application.yml`'deki default literal değil | ✅ startup validator |
| Refresh cookie `Secure=true` | `JWT_REFRESH_COOKIE_SECURE=true` | ✅ startup validator |
| Swagger/OpenAPI kapalı | `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false` (`application-prod.yml`'de zaten set) | ✅ startup validator |
| CORS wildcard değil | `SecurityConfig.corsConfigurationSource()` explicit origin listesi (`afetistanbul.online` + localhost dev origin'leri) | ✅ startup validator |
| Actuator hassas endpoint'ler korunuyor | `/actuator/health` (+`/liveness`,`/readiness`) public, geri kalanı `hasRole('ADMIN')` | Manuel doğrulama (curl) |
| Security header'ları aktif | `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, HSTS (HTTPS'te), CSP (`default-src 'none'`, API JSON döndürdüğü için güvenli) | Manuel (`curl -I`) |
| Rate limiting aktif | login/refresh/search/AI/upload uçları için Bucket4j filtre — bkz. aşağıdaki tablo | Kod incelemesi |
| HTTPS zorunlu | Reverse proxy (nginx) seviyesinde HTTP→HTTPS redirect | Manuel |
| Demo mode kasıtlı | `app.demo-admin-enabled` (salt-okunur "Admin Demo Modu") **kasıtlı olarak** prod'da varsayılan açık — bir güvenlik açığı DEĞİL, `DemoModeWriteGuardFilter` tüm yazma isteklerini son çare olarak keser. `app.demo-users-enabled` (tam demo kullanıcı hesapları) prod'da açıksa bilinçli bir karar olmalı. | Manuel karar |

### Startup validation neyi KONTROL ETMEZ (bilerek)

- DB URL/credential'ların "gerçek" olup olmadığı (env'den geldiği için runtime'da bilinemez).
- Demo mode flag'leri (kasıtlı bir özellik, yanlış config değil — yukarı bakın).
- Storage path'in var/yazılabilir olduğu (Flyway/DB bağlantısı zaten health check ile kapsanıyor).

---

## Database

- [ ] Flyway migration'lar deploy öncesi `mvn flyway:info` ile önizlendi, beklenmeyen "pending" yok.
- [ ] `spring.jpa.hibernate.ddl-auto: validate` — **hiçbir zaman** `update`/`create` prod'da kullanılmaz (zaten sabit, override edilmemiş).
- [ ] Yeni constraint/migration'lar mevcut veriyle test edildi (bkz. `V83__db_integrity_constraints.sql` — negatif stok/duplicate aktif atama temizliği migration içinde otomatik yapılır).
- [ ] Connection pool (`HikariCP`) `maximum-pool-size=20` gerçek DB instance kapasitesiyle uyumlu.
- [ ] **Backup alındı ve restore edilebilirliği doğrulandı** (aşağıdaki Backup/Restore bölümüne bakın).

### Backup / Restore (PostgreSQL, placeholder — gerçek credential yazmayın)

```bash
# Backup (custom format, paralel restore'a uygun)
pg_dump -h <DB_HOST> -p <DB_PORT> -U <DB_USER> -F c -f afet_backup_$(date +%Y%m%d_%H%M).dump <DB_NAME>

# Restore (BOŞ bir DB'ye veya --clean ile mevcut objeleri düşürerek)
pg_restore -h <DB_HOST> -p <DB_PORT> -U <DB_USER> -d <DB_NAME> --clean --if-exists afet_backup_YYYYMMDD_HHMM.dump

# Restore testi ÖNEMLİ: production'ı değil, AYRI bir staging DB'yi hedefleyin,
# uygulamayı o DB'ye karşı ayağa kaldırıp health check + birkaç smoke test çalıştırın.
```

**Backup alınabiliyor olması yetmez — restore prosedürü periyodik olarak (örn. çeyreklik)
staging'de fiilen denenmelidir.** Bu proje kapsamında otomatik bir backup zamanlayıcısı
KURULMADI; mevcut deployment ortamının (VM/managed Postgres) kendi backup mekanizması
kullanılmalı, yukarıdaki komutlar manuel/cron placeholder'ıdır.

---

## Storage (dosya/fotoğraf upload)

| Tür | Max boyut | Content-Type whitelist | Magic-byte doğrulama | Path traversal koruması |
|---|---|---|---|---|
| Belge (kimlik/sertifika) | 10MB | `application/pdf`, `image/png`, `image/jpeg` | ✅ | ✅ (server-generated storage key) |
| Hasar tespiti fotoğrafı | 20MB/foto (multipart request toplamı `spring.servlet.multipart.max-request-size=10MB` ile sınırlı — bkz. Kalan Riskler) | `image/jpeg`, `image/png`, `image/webp` | ✅ | ✅ |
| Saha doğrulama fotoğrafı | aynı | aynı | ✅ | ✅ |
| Toplanma alanı Excel import | 5MB | yalnızca `.xlsx` + ZIP magic-byte (`PK\x03\x04`) | ✅ | N/A (parse edilir, dosya diske yazılmaz) |

- [ ] `.local-storage` dizini (veya `STORAGE_LOCAL_PATH`) prod'da kalıcı bir volume'a bağlı, container restart'ında kaybolmuyor.
- [ ] Bu dizin işletim sistemi seviyesinde yalnızca uygulama kullanıcısına yazılabilir.
- [ ] Private document indirme linkleri artık 24 saat sonra süresi doluyor (`download_token_expires_at`, her yetkili istek rotasyon yapar) — sızan eski bir link süresiz çalışmaz.

---

## Monitoring

- [ ] `/actuator/health` (anonim: yalnızca `{"status":"UP"}`), `/actuator/health/liveness`, `/actuator/health/readiness` uptime monitor'e bağlı.
- [ ] Loglar `requestId` içeriyor (`X-Request-ID` — istemci verirse güvenli formatta kabul edilir, yoksa üretilir, response header'a geri yazılır, MDC'ye eklenir).
- [ ] Log satırları access token / refresh token / password / Authorization header / cookie **içermiyor** (kod incelemesiyle doğrulandı — bkz. rapordaki "Observability" bölümü).
- [ ] Alerting (harici bir servis, örn. UptimeRobot/Grafana/Sentry) health endpoint'ini ve 5xx oranını izliyor — **bu proje kapsamında kurulmadı, ayrıca değerlendirilmeli.**

---

## Frontend

- [ ] `npm run build` prod modda çalıştırıldı, `dist/` içinde secret YOK (Vite env değişkenleri build-time'da bundle'a gömülür ve **public**tir — bu yüzden `VITE_*` altında hiçbir gizli anahtar tutulmaz, kontrol edildi).
- [ ] `VITE_API_BASE_URL` prod'da doğru (nginx reverse-proxy `/api` üzerinden backend'e yönlendiriyorsa boş bırakılabilir — mevcut `.env` deseni budur).
- [ ] Development-only özellik/console debug bayrağı (`VITE_ENABLE_DEVTOOLS`) prod'da `false`.
- [ ] Source map politikası: Vite varsayılanı prod build'de source map ÜRETMEZ (`build.sourcemap` explicit `true` yapılmadıkça) — bu proje bunu override etmiyor, dolayısıyla prod bundle'da source map yok (istenen davranış).
- [ ] Route-level lazy loading ağır sayfalarda (Map, Reports, Admin, Simulations, Coordination Center) aktif — bkz. raporun Performance bölümü.
- [ ] Smoke test: login, dashboard, harita, en az bir mutation (örn. stok güncelleme) manuel denendi.

---

## Operations

- [ ] En az bir gerçek ADMIN hesabı var ve şifresi güvenli (demo hesap değil).
- [ ] `DEMO_USERS_ENABLED` prod'da bilinçli olarak ayarlandı (varsayılan `false`).
- [ ] `DEMO_ADMIN_ENABLED` (salt-okunur halka açık demo) prod'da AÇIK KALMASI kasıtlıysa onaylandı; kapatılması gerekiyorsa `false` set edilir.
- [ ] Emergency rollback prosedürü biliniyor: önceki container image/tag'e dön + gerekiyorsa `pg_restore` (yukarı bakın) — bu proje bir "tek tık rollback" otomasyonu içermiyor, manuel.
- [ ] Reverse proxy (nginx) `X-Forwarded-For` ayarı `TRUSTED_PROXIES` env değişkeniyle backend'e bildirildi (aksi halde rate limiting yanlış IP'yi baz alır — bkz. `ClientIpResolver`).

---

## Rate Limiting — endpoint → limit tablosu

Tek-instance, in-memory (Bucket4j). **Multi-instance deployment'ta her instance kendi
sayacını tutar — gerçek limit instance sayısıyla orantılı gevşer.** Yatay ölçekleme
planlanıyorsa Redis tabanlı distributed bir limiter'a geçilmeli.

| Endpoint | Politika | Limit | Pencere | Anahtar |
|---|---|---|---|---|
| `POST /api/auth/login` | LOGIN | 10 istek | 1 dakika | IP |
| `POST /api/auth/demo-login` | LOGIN | 10 istek | 1 dakika | IP |
| `POST /api/auth/refresh` | REFRESH | 30 istek | 1 dakika | IP |
| `GET /api/search` | SEARCH | 30 istek | 1 dakika | kullanıcı (authenticated) |
| `POST /api/ai/operations-assistant` | AI | 10 istek | 5 dakika | kullanıcı |
| `POST /api/damage-assessments/{id}/ai-analysis` | AI | 10 istek | 5 dakika | kullanıcı |
| `POST /api/damage-assessments/ai/enqueue-missing` | AI | 10 istek | 5 dakika | kullanıcı |
| `POST /api/users/me/documents` | UPLOAD | 20 istek | 1 dakika | kullanıcı |
| `POST /api/damage-assessments` (fotoğraflı oluşturma) | UPLOAD | 20 istek | 1 dakika | kullanıcı |
| `POST /api/my-tasks/damage-assessments/{id}/photos` | UPLOAD | 20 istek | 1 dakika | kullanıcı |

Aşan istek → `429 Too Many Requests` + `Retry-After` header + Türkçe mesaj.

**Not:** SSE/notification-stream bağlantı limiti bu tabloda yok — bu backend'de SSE/WebSocket
implementasyonu bulunmuyor (bildirimler React Query polling ile çekiliyor), dolayısıyla
"aynı kullanıcının sınırsız paralel stream açması" riski bu mimaride mevcut değil.
