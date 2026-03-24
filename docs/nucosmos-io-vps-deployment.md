# nucosmos.io to Namecheap VPS

This document assumes:

- Domain: `nucosmos.io`
- Hosting target: Namecheap Linux VPS
- App root on VPS: `/srv/nucosmos-pos`
- Admin web and API share the same domain

Final URL layout:

- `https://nucosmos.io` -> Vue admin web
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
- If this VPS already uses `WHM/cPanel`, keep host Apache on `80/443` and reverse proxy to `127.0.0.1:8080`
- If this is a plain Linux VPS, you may instead use host Nginx or Caddy for TLS termination

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

Set `ADMIN_WEB_PORT=8080` when host Apache, Nginx, or Caddy will reverse proxy traffic from ports `80/443`.

## 5. Deploy

```bash
cd /srv/nucosmos-pos
chmod +x deployment/deploy.sh
./deployment/deploy.sh
```

## 6. Reverse proxy

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

### Option B: Host Nginx example

If host Nginx handles TLS and reverse proxy:

```nginx
server {
    listen 80;
    server_name nucosmos.io www.nucosmos.io;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Because container Nginx already routes `/api` to backend, the host reverse proxy only needs to proxy everything to `127.0.0.1:8080`.

## 7. HTTPS

Recommended options:

- `certbot --nginx`
- Caddy with automatic TLS
- Cloudflare proxy in front of the VPS

For a simple VPS setup with host Nginx:

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
sudo certbot --nginx -d nucosmos.io -d www.nucosmos.io
```

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
