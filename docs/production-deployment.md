# Production Deployment

This project is now prepared for production deployment on a Linux VPS.

Recommended target:

- Domain: `nucosmos.io`
- Server: Namecheap Linux VPS
- App root: `/srv/nucosmos-pos`
- Production baseline on the current VPS is `Apache/httpd` on `80/443`
- Host `nginx.service` must remain `disabled` so it cannot steal `80/443` from Apache after reboot

## Production files

- [Dockerfile.admin-web](/c:/NUCOSMOS_POS/Dockerfile.admin-web)
- [backend/Dockerfile](/c:/NUCOSMOS_POS/backend/Dockerfile)
- [deployment/docker-compose.prod.yml](/c:/NUCOSMOS_POS/deployment/docker-compose.prod.yml)
- [deployment/.env.prod.example](/c:/NUCOSMOS_POS/deployment/.env.prod.example)
- [deployment/deploy.sh](/c:/NUCOSMOS_POS/deployment/deploy.sh)
- [deployment/nginx/admin-web.conf](/c:/NUCOSMOS_POS/deployment/nginx/admin-web.conf)
- [GitHub Actions deploy workflow](/c:/NUCOSMOS_POS/.github/workflows/deploy-production.yml)

## Recommended URL structure

- `https://nucosmos.io` -> public landing page with member login / redeem entry
- `https://nucosmos.io/erp` -> admin web
- `https://nucosmos.io/api` -> backend API

## Main deployment paths

- VPS deployment guide: [nucosmos-io-vps-deployment.md](/c:/NUCOSMOS_POS/docs/nucosmos-io-vps-deployment.md)
- Apache without cPanel account: [nucosmos-io-apache-without-cpanel.md](/c:/NUCOSMOS_POS/docs/nucosmos-io-apache-without-cpanel.md)
- SFTP upload package: [sftp-deployment-package.md](/c:/NUCOSMOS_POS/docs/sftp-deployment-package.md)
- 2026-03-28 outage and recovery record: [2026-03-28-vps-recovery-incident.md](/c:/NUCOSMOS_POS/docs/2026-03-28-vps-recovery-incident.md)
- 2026-04-04 disk-space recovery SOP: [2026-04-04-postgres-disk-space-recovery.md](/c:/NUCOSMOS_POS/docs/2026-04-04-postgres-disk-space-recovery.md)

## Local validation already completed

- Frontend build passes
- Backend tests pass
- Production compose config renders successfully

Before real deployment, you still need:

- VPS public IP
- SSH user
- DNS A record for `nucosmos.io`
- GitHub repository secrets for SSH deployment
- Reverse proxy from host Apache to `127.0.0.1:8080`

## Current production note

The currently verified production routing on the VPS is:

- Apache serves the landing page from `/var/www/nucosmos-cover`
- Repo-managed landing assets live in `deployment/public-site/` and are synced by `deployment/deploy.sh`
- Repo-managed Apache vhost can be synced and reloaded by `deployment/deploy.sh` when `.env.prod` sets `APACHE_VHOST_TARGET`
- Apache proxies `/erp/` to `127.0.0.1:8080`
- Apache proxies `/api/` to `127.0.0.1:8080/api/`
- Apache proxies `/redeem/` to `127.0.0.1:8080/redeem/`
- container Nginx then forwards `/api/` to the backend container
- container Nginx forwards `/redeem/` to the backend container

This is the path that was validated after VPS recovery and should be treated as the canonical
reference unless a future deployment intentionally changes it.

For the current cPanel/EA4 VPS:

- live Apache include path: `/etc/apache2/conf.d/includes/post_virtualhost_global.conf`
- repo source: [nucosmos.io.conf](/c:/NUCOSMOS_POS/deployment/apache/nucosmos.io.conf)
- recommended `.env.prod` setting:

```env
APACHE_VHOST_TARGET=/etc/apache2/conf.d/includes/post_virtualhost_global.conf
```

## Apache-only production rule

The current production VPS must follow this rule:

- host `Apache/httpd` is the only public web server
- host `nginx.service` must stay `disabled`
- container nginx is allowed only inside the `admin-web` Docker image

This distinction is important:

- host Apache owns public `80/443`
- Docker `admin-web` exposes internal app traffic on `:8080`
- Docker-internal nginx is not the same thing as host `nginx.service`

## Reboot recovery guardrail

If the VPS reboots and the public sites come back as an nginx default page or HTTPS stops listening,
check service ownership before redeploying the app:

```bash
systemctl status httpd --no-pager
systemctl status nginx --no-pager
ss -ltnp | grep -E ':80|:443'
```

Expected steady state:

- `httpd` is `active (running)`
- `nginx` is `inactive` or `disabled`
- `80/443` are owned by `httpd`

If host nginx has taken port `80`, restore the expected state:

```bash
systemctl stop nginx
systemctl disable nginx
systemctl enable httpd
systemctl start httpd
apachectl -t
ss -ltnp | grep -E ':80|:443'
```

If Docker redeploy then fails with `overlay2 ... device or resource busy`, check whether cPanel
`virtfs` has mounted the old Docker overlay filesystem under `/home/virtfs/...` before retrying
container removal. See the incident record:

- [2026-03-28-vps-recovery-incident.md](/c:/NUCOSMOS_POS/docs/2026-03-28-vps-recovery-incident.md)

## VPS memory stability note

During the `2026-03-25` to `2026-03-26` production rollout, the VPS became unstable because:

- total RAM was about `2 GB`
- swap was initially `0`
- the Linux OOM killer terminated `java`, `php-cgi`, and `mariadbd`

The production baseline is now expected to include:

- a `2 GB` swap file
- `vm.swappiness=10`
- backend JVM limits through `JAVA_TOOL_OPTIONS`

Recommended production JVM settings:

```text
-Xms128m -Xmx384m -XX:MaxMetaspaceSize=192m
```

These values are now reflected in the production compose defaults.

## VPS disk-space guardrail

This VPS also needs an explicit disk-space operating rule:

- keep at least `10 GB` free on `/`
- if root free space falls under roughly `15%`, clean Docker build cache before the next deploy
- if `postgres` becomes unhealthy and logs show `No space left on device`, follow:
  - [2026-04-04-postgres-disk-space-recovery.md](/c:/NUCOSMOS_POS/docs/2026-04-04-postgres-disk-space-recovery.md)

Typical triage commands:

```bash
df -h
docker system df
du -xh /var/lib/docker --max-depth=1 2>/dev/null | sort -h
```

## Optional admin cache cleanup

The admin web can expose an `ADMIN`-only "Docker cache cleanup" action, but it is intentionally
disabled by default.

To enable it on the VPS:

```env
DOCKER_MAINTENANCE_ENABLED=true
DOCKER_BINARY_PATH=/usr/bin/docker
DOCKER_SOCKET_PATH=/var/run/docker.sock
```

This feature is intentionally limited to the following safe commands:

- `docker builder prune -af`
- `docker image prune -af`
- `docker container prune -f`

It must never run:

- `docker volume prune`
- arbitrary shell commands

Security note:

- enabling this feature mounts `/var/run/docker.sock` into the backend container
- only `ADMIN` users should have access to the cleanup button
- POS should remain monitor-only for disk status and should not get host-maintenance controls
