# 2026-03-26 Operations And Security Update

This note summarizes the production, inventory, and authentication work completed on `2026-03-26`.

## 1. Production And VPS Changes

### Public routing now in use

- `https://nucosmos.io/` serves the temporary landing page
- `https://nucosmos.io/erp/` serves the admin web
- `https://nucosmos.io/api/` serves the backend API

### Apache and container routing

The currently validated production chain is:

- Apache serves `/` from `/var/www/nucosmos-cover`
- Apache proxies `/erp/` to `http://127.0.0.1:8080/`
- Apache proxies `/api/` to `http://127.0.0.1:8080/api/`
- the `admin-web` container Nginx proxies `/api/` onward to `backend:8081`

This path is the canonical production reference after VPS recovery.

### SSL

- `nucosmos.io` is now served over HTTPS with the manually installed certificate
- HTTP requests are redirected to HTTPS

### VPS recovery findings

The VPS instability was traced to memory pressure:

- VPS RAM: about `2 GB`
- swap before fix: `0`
- Linux OOM killer terminated `java`, `php-cgi`, and `mariadbd`

### Stability work completed

- added `2 GB` swap on the VPS
- reduced Linux `vm.swappiness` to `10`
- capped production backend JVM memory with:
  - `-Xms128m`
  - `-Xmx384m`
  - `-XX:MaxMetaspaceSize=192m`
- updated `deployment/deploy.sh` to retry backend health checks for up to `60` seconds instead of failing too early during startup

### Reboot recovery sequence that worked

If the VPS reboots and Docker networking is broken, this sequence recovered the stack:

```bash
systemctl start docker
cd /srv/nucosmos-pos
docker rm -f nucosmos-pos-postgres-prod nucosmos-pos-backend-prod nucosmos-pos-admin-web-prod 2>/dev/null || true
docker network rm deployment_default 2>/dev/null || true
docker network prune -f
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml up -d
systemctl restart httpd
```

## 2. Inventory And Product Model Changes

### New inventory type

The system now supports a third inventory class in addition to materials and packaging:

- `MANUFACTURED`

This type was added across:

- backend domain and migrations
- admin inventory management
- replenishment and procurement
- POS quick receive

### POS quick receive

POS quick receive now supports:

- `MATERIAL`
- `MANUFACTURED`
- `PACKAGING`

It can also create new quick receive items directly from POS instead of requiring the item to already exist.

### Product recipes

Product recipes now support `MANUFACTURED` as a first-class recipe section instead of forcing it under `MATERIAL`.

The supported recipe sections are now:

- materials
- manufactured items
- packaging

## 3. Login And PIN Changes

### Admin web login

The admin login flow was simplified:

- remove manual role selection from login
- remove exposed demo PINs from the login screen
- submit only `storeCode + pin + deviceCode`
- let the backend resolve the active role automatically

### Backend PIN protection

The backend now includes temporary throttling for repeated PIN failures:

- scoped by `storeCode + client IP`
- after `5` failed attempts
- lockout duration: `15 minutes`
- HTTP response: `429 Too Many Requests`

### POS login hardening

POS login was hardened with two changes:

- production builds no longer allow editing the API base URL on-device
- the keypad now supports `4-6` digits instead of hard-locking the flow to exactly `4`

### Current status of the PIN security work

The PIN hardening changes above are implemented locally and verified with:

- `npm run build`
- `backend\\mvnw.cmd -q test`
- `flutter analyze`
- `flutter test`

At the time of writing this note, those security edits are documented here but still need their own `commit + push` before VPS deployment.

## 4. Next Auth Direction

The next planned auth change is:

- move from `4-6` digits to `6 digits only`
- add first-time registration with:
  - phone number
  - 6-digit PIN
  - Firebase SMS verification
- allow later login with phone number and PIN after the registration is verified
