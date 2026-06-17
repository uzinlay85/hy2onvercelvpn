# SafeNet VPN Platform

SafeNet VPN is a full-stack VPN management platform for a production VPS setup. It includes a NestJS backend API, a Next.js admin dashboard, an Android VPN client, PostgreSQL, Redis, Nginx HTTPS routing, and AmneziaWG Easy integration for real VPN tunnel provisioning.

SafeNet VPN သည် VPS production server ပေါ်တွင် တင်သုံးရန် ရည်ရွယ်ထားသော VPN management platform ဖြစ်သည်။ NestJS backend API, Next.js admin dashboard, Android app, PostgreSQL, Redis, Nginx HTTPS gateway နှင့် AmneziaWG Easy VPN backend တို့ ပါဝင်သည်။

## Current Production Flow

1. Android app opens and creates or reuses a stable device identity.
2. App calls `POST /api/v1/auth/device/bootstrap`.
3. Backend creates or reuses the device user and returns JWT tokens.
4. Android stores tokens securely and syncs user/server/subscription data.
5. Admin dashboard shows the user/device and can assign plans or manage status.
6. User taps Connect.
7. Backend selects an eligible `AMNEZIA_WG` server, creates or reuses an AmneziaWG Easy client key, and returns the tunnel config.
8. Android starts the native AmneziaWG tunnel and reports traffic back to the API.

မြန်မာအကျဉ်းချုပ်:

- App ကိုဖွင့်ရုံဖြင့် device bootstrap အလိုအလျောက်လုပ်သည်။
- Admin dashboard ထဲတွင် user/device ပေါ်လာပြီး plan သတ်မှတ်နိုင်သည်။
- Connect နှိပ်ပါက backend က AmneziaWG Easy မှ client key ထုတ်ပြီး Android app သို့ VPN config ပြန်ပေးသည်။
- Android app က real AmneziaWG tunnel ကိုစတင်ပြီး usage traffic ကို backend သို့ report လုပ်သည်။

## Tech Stack

| Area | Stack |
| --- | --- |
| Backend | NestJS, Prisma, PostgreSQL, Redis, JWT, WebSocket |
| Admin panel | Next.js 14, React, Tailwind CSS, React Query, Recharts |
| Android app | Kotlin, Jetpack Compose, Hilt, Retrofit, Room, WorkManager, AmneziaWG Android backend |
| VPN backend | AmneziaWG Easy |
| Edge gateway | Nginx with HTTPS, WebSocket upgrade, rate limiting |
| Deployment | Docker Compose on Ubuntu VPS |

## Repository Structure

```text
.
├── backend/                 NestJS API, Prisma schema, auth, plans, servers, VPN logic
├── admin-panel/             Next.js admin dashboard
├── android/                 Android VPN client app
├── devops/                  Nginx config and operational scripts
├── play-store-assets/       Store listing screenshots/assets
├── tools/                   Utility scripts
├── docker-compose.yml       Production Docker Compose stack
├── README.md                Main project overview
├── Runbook.md               Day-to-day production checks and troubleshooting
├── VPS_NEW_SERVER_MIGRATION_RUNBOOK.md
│                            Clean install guide for a new VPS
├── VPS_SECURITY_BASELINE_7_STEPS.md
│                            VPS security hardening guide
└── vps_deployment_guide.md  Short deployment guide
```

## Production URLs

| Purpose | URL |
| --- | --- |
| Public site/admin | `https://safenetapp.truehand.top` |
| Admin login | `https://safenetapp.truehand.top/login` |
| API base | `https://safenetapp.truehand.top/api/v1` |
| WebSocket base | `wss://safenetapp.truehand.top` |

Android build config currently points to:

```kotlin
API_BASE_URL = "https://safenetapp.truehand.top/api/v1"
WS_BASE_URL = "wss://safenetapp.truehand.top"
```

## Docker Services

| Service | Container | Internal Port | Public Exposure |
| --- | --- | ---: | --- |
| PostgreSQL | `safenet_postgres` | `5432` | Internal Docker network only |
| Redis | `safenet_redis` | `6379` | Internal Docker network only |
| Backend API | `safenet_api` | `3000` | Through Nginx `/api/*` only |
| Admin panel | `safenet_admin` | `3001` | Through Nginx `/` only |
| Nginx | `safenet_nginx` | `80`, `443` | Public HTTP/HTTPS |
| AmneziaWG Easy | `amnezia-wg-easy` | `51831`, `58210/udp` | `58210/udp` public for VPN tunnel |

Production hardening expectation: ports `3000`, `3001`, `5432`, and `6379` should not be publicly listening on the VPS host.

## Environment

Create `backend/.env` from `backend/.env.example`, then update values for production. Minimum important values:

```env
NODE_ENV=production
APP_URL=https://safenetapp.truehand.top

POSTGRES_USER=safenet
POSTGRES_PASSWORD=CHANGE_THIS_DB_PASSWORD
POSTGRES_DB=safenet_vpn
DATABASE_URL="postgresql://safenet:CHANGE_THIS_DB_PASSWORD@postgres:5432/safenet_vpn?schema=public"

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=CHANGE_THIS_REDIS_PASSWORD

JWT_SECRET=PASTE_64_CHAR_HEX_SECRET
JWT_REFRESH_SECRET=PASTE_ANOTHER_64_CHAR_HEX_SECRET
ENCRYPTION_KEY=PASTE_64_CHAR_HEX_ENCRYPTION_KEY

ADMIN_EMAIL=YOUR_ADMIN_EMAIL
ADMIN_PASSWORD=YOUR_STRONG_ADMIN_PASSWORD

AMNEZIA_API_URL=http://amnezia-wg-easy:51831
AMNEZIA_USERNAME=admin
AMNEZIA_PASSWORD=YOUR_AMNEZIA_PLAIN_PASSWORD
AMNEZIA_SERVER_ID=

CORS_ORIGINS=https://safenetapp.truehand.top
```

Use strong random values:

```bash
openssl rand -hex 32
```

`AMNEZIA_PASSWORD` must be the original plain Web UI password, not the `PASSWORD_HASH` used by the AmneziaWG Easy container.

## VPS Deployment Summary

For a fresh server, follow these docs in order:

1. `VPS_SECURITY_BASELINE_7_STEPS.md`
2. `VPS_NEW_SERVER_MIGRATION_RUNBOOK.md`
3. `Runbook.md` for daily checks and troubleshooting

Short version:

```bash
cd ~/SafeNetVPN
docker compose up -d --build postgres redis api admin nginx
docker compose ps
docker compose logs --tail=120 api
```

For AmneziaWG Easy, run it on the same Docker network as SafeNet and use:

```text
Management URL: http://amnezia-wg-easy:51831
Public VPN host: safenetapp.truehand.top
VPN UDP port: 58210
Provider: AMNEZIA_WG_EASY
```

## Admin Dashboard

Admin login uses `ADMIN_EMAIL` and `ADMIN_PASSWORD` from `backend/.env`.

Main workflows:

- View users, devices, sessions, keys, plans, payments, and analytics.
- Assign subscription plans and control user status.
- Add or update AmneziaWG Easy server records.
- Queue self-hosted SafeNet notifications. Android clients sync these from the VPS API and show local notifications.
- Monitor sessions and traffic usage.

## Android App

Key behaviors:

- Automatic device bootstrap, no manual registration required.
- Native AmneziaWG tunnel via the Android backend library.
- Encrypted token storage.
- Plan/subscription view with Myanmar Kyat pricing.
- Notification sync through SafeNet backend API, not Firebase broadcast delivery.
- Traffic reporting to backend while connected.

Build debug APK on Windows:

```powershell
cd C:\Users\zin\Downloads\Ai_WebCodes\SafeNetapp_ByZin\android
.\gradlew.bat :app:assembleDebug
```

Debug APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Release builds should be generated with Android Studio or the configured Gradle release task/key setup.

## Useful Checks

Production health:

```bash
cd ~/SafeNetVPN
docker compose ps
docker compose logs --tail=120 api
curl -I https://safenetapp.truehand.top
```

API can reach AmneziaWG Easy:

```bash
docker exec safenet_api sh -lc 'node -e "fetch(\"http://amnezia-wg-easy:51831/\").then(r=>console.log(r.status, r.headers.get(\"content-type\"))).catch(e=>console.error(e.message))"'
```

Expected:

```text
200 text/html
```

Verify real tunnel handshakes:

```bash
docker exec amnezia-wg-easy wg show
```

Expected:

- `interface: wg0`
- `listening port: 58210`
- at least one peer after a client connects
- `latest handshake` updates after Android connects
- transfer counters increase while browsing through VPN

## Troubleshooting Docs

- Daily production checks: `Runbook.md`
- New VPS clean install: `VPS_NEW_SERVER_MIGRATION_RUNBOOK.md`
- VPS security baseline: `VPS_SECURITY_BASELINE_7_STEPS.md`
- Short VPS deployment guide: `vps_deployment_guide.md`
- DevOps notes: `devops/README.md`

Do not commit local secrets, `.env` files, generated APK/AAB files, Android build cache files, or local IDE metadata unless intentionally requested.
