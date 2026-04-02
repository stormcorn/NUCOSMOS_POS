# nucosmos.io to Namecheap VPS

This document assumes:

- Domain: `nucosmos.io`
- Hosting target: Namecheap Linux VPS
- App root on VPS: `/srv/nucosmos-pos`
- Admin web and API share the same domain

Final URL layout:

- `https://nucosmos.io` -> public landing page with member login / redeem entry
- `https://nucosmos.io/api` -> Spring Boot backend

## 1. DNS

In Namecheap DNS settings for `nucosmos.io`:

- `A` record: `@` -> your VPS public IP
- `A` record: `www` -> your VPS public IP, or redirect `www` to apex

Wait until DNS propagation is complete.

## 2. Required software on VPS

Install:

- Docker Engine
- Docker Compose plugin
- Git
- Host `Apache/httpd` on `80/443`

Current mandatory rule for the production VPS:

- host `Apache/httpd` is the only public web server
- host `nginx.service` must remain `disabled`
- Docker-internal nginx inside `admin-web` is allowed, but it does not replace host Apache

## 3. Upload project to VPS

Option A:

- Use GitHub Actions workflow in [deploy-production.yml](/c:/NUCOSMOS_POS/.github/workflows/deploy-production.yml)

Option B:

- Use the SFTP package described in [sftp-deployment-package.md](/c:/NUCOSMOS_POS/docs/sftp-deployment-package.md)

## 4. Production env

Create:

```bash
/srv/nucosmos-pos/deployment/.env.prod
```

Recommended values:

```env
POSTGRES_DB=nucosmos_pos
POSTGRES_USER=nucosmos
POSTGRES_PASSWORD=replace_with_strong_postgres_password

DB_NAME=nucosmos_pos
DB_USERNAME=nucosmos
DB_PASSWORD=replace_with_strong_postgres_password

JWT_SECRET_BASE64=replace_with_a_real_base64_secret
JWT_ACCESS_TOKEN_MINUTES=480

FRONTEND_ORIGIN=https://nucosmos.io
VITE_API_BASE_URL=
ADMIN_WEB_PORT=8080
```

Set `ADMIN_WEB_PORT=8080` because host Apache will reverse proxy traffic from ports `80/443`.
`deployment/deploy.sh` will also sync `deployment/public-site/` into `/var/www/nucosmos-cover`.

## 5. Deploy

```bash
cd /srv/nucosmos-pos
chmod +x deployment/deploy.sh
./deployment/deploy.sh
```

## 6. Reverse proxy

If `nucosmos.io` will **not** be added as a cPanel-managed domain, use:

- [nucosmos-io-apache-without-cpanel.md](/c:/NUCOSMOS_POS/docs/nucosmos-io-apache-without-cpanel.md)

### Option A: WHM/cPanel Apache reverse proxy

Find the cPanel account that owns `nucosmos.io`:

```bash
grep nucosmos.io /etc/userdomains
```

Assume the cPanel account is `CPANELUSERNAME`, then create the Apache userdata include files:

```bash
mkdir -p /etc/apache2/conf.d/userdata/std/2_4/CPANELUSERNAME/nucosmos.io
mkdir -p /etc/apache2/conf.d/userdata/ssl/2_4/CPANELUSERNAME/nucosmos.io
```

Create both files with the same content:

```apache
ProxyPass /.well-known !
ProxyPass / http://127.0.0.1:8080/
ProxyPassReverse / http://127.0.0.1:8080/
```

Apply the Apache config:

```bash
/usr/local/cpanel/scripts/rebuildhttpdconf
/usr/local/cpanel/scripts/restartsrv_httpd
```

## 7. HTTPS

Recommended options:

- Apache-managed certificate on the VPS
- Cloudflare proxy in front of the VPS

Do not enable host nginx for TLS termination on this production server.

## 8. GitHub Actions secrets

Add these secrets in GitHub repository settings:

- `VPS_HOST`
- `VPS_USER`
- `VPS_SSH_PRIVATE_KEY`

The workflow will:

- sync the project to `/srv/nucosmos-pos`
- run `deployment/deploy.sh`

## 9. Health checks

After deployment:

```bash
docker compose --env-file /srv/nucosmos-pos/deployment/.env.prod -f /srv/nucosmos-pos/deployment/docker-compose.prod.yml ps
curl http://127.0.0.1:8080
curl http://127.0.0.1:8080/actuator/health
```

## 10. Reboot ownership check

After every VPS reboot, verify that Apache still owns the public ports:

```bash
systemctl status httpd --no-pager
systemctl status nginx --no-pager
ss -ltnp | grep -E ':80|:443'
```

Expected:

- `httpd` is running
- `nginx` is disabled or inactive
- `80/443` are bound by `httpd`

## 11. If Docker redeploy is blocked by overlay removal

On this VPS, cPanel jailed-shell `virtfs` can keep old Docker overlay directories mounted under:

```text
/home/virtfs/<cpanel-user>/var/lib/docker/overlay2/.../merged
```

Typical failure:

```text
driver "overlay2" failed to remove root filesystem ... device or resource busy
```

If this happens:

```bash
mount | grep '/home/virtfs'
umount -l /home/virtfs/<cpanel-user>/var/lib/docker/overlay2/<overlay-id>/merged
docker rm -f <stuck-container-id>
systemctl restart docker
```

Then recreate the affected services:

```bash
cd /srv/nucosmos-pos
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml up -d --build backend admin-web
```

See the full incident record:

- [2026-03-28-vps-recovery-incident.md](/c:/NUCOSMOS_POS/docs/2026-03-28-vps-recovery-incident.md)
