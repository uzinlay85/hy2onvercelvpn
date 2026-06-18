# Zin SafeNet V2 Platform

Zin SafeNet V2 is a comprehensive full-stack VPN platform optimized for a production VPS. It features a NestJS backend API, a Next.js admin dashboard, and an Android client that supports both the high-performance **AmneziaWG** protocol and a modern, high-speed multi-protocol proxy core via **sing-box** (supporting **Hysteria2**, **VLESS**, and **Shadowsocks**).

Zin SafeNet V2 သည် VPS production server ပေါ်တွင် တင်သုံးရန် ရည်ရွယ်ထားသော VPN management platform ဖြစ်သည်။ NestJS backend API, Next.js admin dashboard, Android app, PostgreSQL, Redis, Nginx HTTPS gateway အပြင် AmneziaWG VPN backend နှင့် sing-box multi-protocol proxy backend (Hysteria2, VLESS, Shadowsocks) တို့ကို အပြည့်အဝ ထောက်ပံ့ပေးထားသည်။

---

## Current Production Flow

1. **App Bootstrap**: The Android app launches and establishes or reuses a secure device identity.
2. **Authentication**: App calls `POST /api/v1/auth/device/bootstrap` and gets JWT tokens from the backend.
3. **Synchronization**: Android stores tokens securely, syncing subscription details and available servers.
4. **Admin Management**: The admin dashboard allows the owner to assign subscription plans, block devices, and configure servers.
5. **Connection**: When the user taps Connect:
   - **AmneziaWG**: Backend selects a server, provisions a client key from AmneziaWG Easy, and returns the config.
   - **Multi-Protocol (sing-box)**: Backend serves the connection URI (e.g., `hysteria2://...`, `vless://...`, `ss://...`), which the app converts to a compatible JSON configuration and runs locally via the embedded native `libbox` core.
6. **Data Protection & Routing**:
   - The app dynamically establishes a local Android `VpnService` TUN interface.
   - All outbound traffic from the proxy core is protected via `VpnService.protect(socketFd)` in the bridge to prevent infinite routing loops.
   - Core cache databases are directed to the app's sandboxed writeable cache directory (`cacheDir`) to avoid read-only system crashes.
   - DNS queries are routed dynamically through the tunnel using plain UDP `8.8.8.8` detouring to bypass DNS-over-TLS blocks imposed by local ISP firewalls.

---

## Tech Stack

| Area | Stack |
| --- | --- |
| **Backend** | NestJS, Prisma, PostgreSQL, Redis, JWT, WebSockets |
| **Admin Panel** | Next.js 14, React, Tailwind CSS, React Query, Recharts |
| **Android Client** | Kotlin, Jetpack Compose, Hilt, Retrofit, Room, WorkManager, AmneziaWG Android SDK, sing-box core (`libbox.aar` native bindings) |
| **VPN Backends** | AmneziaWG Easy (WireGuard fork), Hysteria2 (QUIC), VLESS (Reality/WS/gRPC), Shadowsocks |
| **Edge Gateway** | Nginx with SSL/HTTPS, WebSocket upgrade, Rate Limiting |
| **Deployment** | Docker Compose on Ubuntu VPS |

---

## Repository Structure

```text
.
├── backend/                 NestJS API, Prisma schema, auth, plans, servers, VPN logic
├── admin-panel/             Next.js admin dashboard
├── android/                 Android VPN client app (Zin SafeNet V2)
├── devops/                  Nginx configuration and operational scripts
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

---

## Production URLs

| Purpose | URL |
| --- | --- |
| **Public Site/Admin** | `https://safenetapp.truehand.top` |
| **Admin Login** | `https://safenetapp.truehand.top/login` |
| **API Base** | `https://safenetapp.truehand.top/api/v1` |
| **WebSocket Base** | `wss://safenetapp.truehand.top` |

Android build config currently points to:

```kotlin
API_BASE_URL = "https://safenetapp.truehand.top/api/v1"
WS_BASE_URL = "wss://safenetapp.truehand.top"
```

---

## Docker Services

| Service | Container | Internal Port | Public Exposure |
| --- | --- | ---: | --- |
| **PostgreSQL** | `safenet_postgres` | `5432` | Internal Docker network only |
| **Redis** | `safenet_redis` | `6379` | Internal Docker network only |
| **Backend API** | `safenet_api` | `3000` | Through Nginx `/api/*` only |
| **Admin Panel** | `safenet_admin` | `3001` | Through Nginx `/` only |
| **Nginx** | `safenet_nginx` | `80`, `443` | Public HTTP/HTTPS |
| **AmneziaWG Easy** | `amnezia-wg-easy` | `51831`, `58210/udp` | `58210/udp` public for VPN tunnel |

---

## Android Client Setup

### Key Features
- **Multi-Protocol**: Supports AmneziaWG, Hysteria2, VLESS, and Shadowsocks protocols.
- **Application ID**: Configured as `com.zinsafenet.v2` to avoid conflicts with previous installations of SafeNet apps, allowing side-by-side installations.
- **Display Name**: `Zin SafeNet V2`
- **Aesthetics**: Premium violet-blue-cyan linear gradient shield icon matching a modern, clean, and unique dark UI style.
- **Google Play Store / Android 11+ Workarounds**:
  - **Netlink Socket Bypass**: `auto_detect_interface` is set to `false` in routing options to prevent standard Android apps from crashing due to Google's ban on raw netlink sockets (`AF_NETLINK`).
  - **Socket Protection**: Employs a Kotlin reflection proxy wrapper to interface with `Libbox.setup` (`isUser = true`) and capture outbound sockets, calling `VpnService.protect(socketFd)` directly from Java/Kotlin.
  - **Local Path Resolution**: Dynamic database cache files (`cache.db`) are written to the application's internal private cache directory (`context.cacheDir`) rather than the read-only root directory `/`.
  - **DNS Hijack Workaround**: Resolves remote DNS queries over the tunnel using `8.8.8.8` to bypass ISP-level DNS-over-TLS (DoT) blockades.

### Building the Debug APK on Windows

Run the following commands in PowerShell:

```powershell
cd C:\Users\zin\Downloads\Ai_WebCodes\Hy2_app_Safenetapp_V2\android
.\gradlew.bat :app:assembleDebug
```

Debug APK output location:
```text
android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Environment Setup

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

Use `openssl rand -hex 32` to generate secure secrets. `AMNEZIA_PASSWORD` must be the original plain Web UI password, not the `PASSWORD_HASH` used by the AmneziaWG Easy container.

---

## Useful Checks

Production container status:
```bash
cd ~/SafeNetVPN
docker compose ps
docker compose logs --tail=120 api
curl -I https://safenetapp.truehand.top
```

Confirm NestJS API can reach AmneziaWG Easy:
```bash
docker exec safenet_api sh -lc 'node -e "fetch(\"http://amnezia-wg-easy:51831/\").then(r=>console.log(r.status, r.headers.get(\"content-type\"))).catch(e=>console.error(e.message))"'
```
Expected: `200 text/html`

Verify WireGuard interface handshakes:
```bash
docker exec amnezia-wg-easy wg show
```

---

## Reference Manuals & Runbooks
- **Daily Operations & Troubleshooting**: `Runbook.md`
- **Clean VPS Setup**: `VPS_NEW_SERVER_MIGRATION_RUNBOOK.md`
- **VPS Hardening**: `VPS_SECURITY_BASELINE_7_STEPS.md`
- **Short Deployment Guide**: `vps_deployment_guide.md`
- **DevOps Notes**: `devops/README.md`

---
*Note: Make sure `apk_key/` and local credential/configuration files like `hy2keyfile` are ignored in `.gitignore` and never committed to public version control.*
