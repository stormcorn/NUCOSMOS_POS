# 2026-03-28 VPS Recovery Incident

This document records the production outage and recovery steps that occurred on `2026-03-28`
for the `nucosmos.io` VPS.

## Summary

Observed symptoms:

- `https://nucosmos.io` and `https://vtuberonline.com` were unreachable or returned the wrong page
- public `80` responded with a default nginx page
- public `443` was not listening
- Docker deploys could build images successfully, but `backend` and `admin-web` containers could not be recreated cleanly

## Root causes

There were two separate host-level problems.

### 1. Host nginx started after reboot and blocked Apache

The production VPS is supposed to expose public traffic through host `Apache/httpd`.
After reboot:

- `nginx.service` started automatically because it was still `enabled`
- nginx took port `80`
- `httpd` failed with `Address already in use`
- public traffic no longer reached the Apache vhosts for `nucosmos.io` and `vtuberonline.com`

Recovery:

```bash
systemctl stop nginx
systemctl disable nginx
systemctl enable httpd
systemctl start httpd
apachectl -t
ss -ltnp | grep -E ':80|:443'
```

Expected steady state:

- host `httpd` owns `80/443`
- host `nginx.service` stays `disabled`

### 2. cPanel virtfs held old Docker overlay mounts open

During redeploy, Docker failed to remove old container filesystems with:

```text
driver "overlay2" failed to remove root filesystem:
unlinkat .../merged: device or resource busy
```

The old container overlay directories were still mounted under cPanel jailed-shell paths:

```text
/home/virtfs/stormcorn/var/lib/docker/overlay2/.../merged
```

This blocked removal of old `backend` and `admin-web` containers.

Recovery:

1. identify the stuck overlay path
2. lazy-unmount the matching virtfs mount
3. remove the dead container
4. recreate the service

Example commands that were used:

```bash
mount | grep '/home/virtfs/stormcorn/var/lib/docker/overlay2'
umount -l /home/virtfs/stormcorn/var/lib/docker/overlay2/<overlay-id>/merged
docker rm -f <dead-container-id>
systemctl restart docker
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml up -d --build backend admin-web
```

## Reliable recovery checklist

### A. Check public web-server ownership first

```bash
systemctl status httpd --no-pager
systemctl status nginx --no-pager
ss -ltnp | grep -E ':80|:443'
```

Expected:

- `httpd` is running
- `nginx` is disabled or inactive
- `80/443` are bound by `httpd`

### B. Check Docker service state

```bash
systemctl status docker --no-pager
docker ps
docker ps -a --no-trunc --format '{{.ID}}  {{.Names}}  {{.Status}}'
```

### C. If a container is stuck in `Removal In Progress`

```bash
docker inspect <container-id> --format '{{.State.Status}} {{.State.Error}}'
mount | grep '/home/virtfs/stormcorn/var/lib/docker/overlay2'
```

If a matching `virtfs` mount exists:

```bash
umount -l /home/virtfs/stormcorn/var/lib/docker/overlay2/<overlay-id>/merged
docker rm -f <container-id>
```

### D. Restart only after stale mounts are cleared

```bash
systemctl restart docker
cd /srv/nucosmos-pos
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml up -d --build postgres backend admin-web
```

## Correct health checks

Use these checks after recovery:

```bash
docker ps
curl -I --max-time 10 http://127.0.0.1:8080
curl -I --max-time 10 https://nucosmos.io/erp/
docker inspect nucosmos-pos-backend-prod --format '{{.State.Health.Status}}'
docker exec nucosmos-pos-backend-prod sh -lc 'curl -fsS http://127.0.0.1:8081/actuator/health'
```

Notes:

- `backend` should be healthy inside the Docker network on `backend:8081`
- `admin-web` should listen on `:8080`
- Apache should route `/erp/` publicly to the Dockerized admin web

## Lessons captured as deployment rules

- Host production traffic on this VPS must remain Apache-only
- Host `nginx.service` must stay disabled
- Docker container nginx inside `admin-web` is allowed and expected
- cPanel jailed-shell `virtfs` can hold old Docker overlay mounts open after container removal
- When redeploy fails with `overlay2 ... device or resource busy`, check `virtfs` mounts before assuming the app image is broken
- On this cPanel/EA4 VPS, `nucosmos.io` Apache vhosts should be synced to `/etc/apache2/conf.d/includes/post_virtualhost_global.conf`
- `nucosmos.io` should bind explicitly to `63.250.42.132` and `127.0.0.1` on `80/443`; wildcard SSL bindings can fall through to other hosted vhosts
