# 2026-04-04 Postgres Disk Space Recovery

This SOP records the production incident on `2026-04-04` where the VPS root disk reached `100%`
usage and PostgreSQL could not complete crash recovery.

## Symptoms

- `docker compose up -d` reported `container nucosmos-pos-postgres-prod is unhealthy`
- `backend` and `admin-web` could be recreated, but startup was blocked by the unhealthy database
- PostgreSQL logs showed repeated recovery loops and checkpoint failures

Key log line:

```text
PANIC: could not write to file "pg_logical/replorigin_checkpoint.tmp": No space left on device
```

## Root cause

The VPS root filesystem `/dev/vda2` reached `100%` usage. PostgreSQL needed to write recovery and
checkpoint files but could not allocate disk space, so it kept restarting in recovery mode.

On this server, the fastest reclaimable space came from Docker:

- old image layers
- build cache
- stale containers

## Immediate recovery steps

### 1. Confirm the disk is full

```bash
df -h
df -i
docker system df
du -xh /var/lib/docker --max-depth=1 2>/dev/null | sort -h
```

Expected failure pattern:

- `/` near or at `100%`
- `inode` usage still normal
- large Docker build cache and old image usage

### 2. Reclaim Docker space safely

Do **not** prune volumes unless you have a verified database backup.

```bash
docker builder prune -af
docker image prune -af
docker container prune -f
df -h
docker system df
```

### 3. Restart PostgreSQL only

```bash
docker start nucosmos-pos-postgres-prod
docker logs --tail 50 nucosmos-pos-postgres-prod
```

Healthy recovery is confirmed when logs include:

```text
checkpoint complete
database system is ready to accept connections
```

### 4. Bring the full stack back

```bash
cd /srv/nucosmos-pos
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml up -d
docker ps
curl -k -I --max-time 10 https://nucosmos.io/redeem/
curl -k -I --max-time 10 https://nucosmos.io/erp/
```

## Safe commands

Recommended cleanup commands:

```bash
docker builder prune -af
docker image prune -af
docker container prune -f
journalctl --vacuum-time=7d
```

Avoid this unless you explicitly intend to destroy data:

```bash
docker volume prune
```

## Prevention rules

- Keep at least `10 GB` free on the VPS root disk during active releases.
- Docker build cache should be cleaned periodically on this server.
- If `postgres` is unhealthy after a deploy, check disk space before changing app configs.
- If PostgreSQL logs include `No space left on device`, treat it as a host-level storage incident, not an app bug.
- The admin web and POS now expose storage status so low-space warnings can be seen before the VPS reaches `100%`.

## Recommended periodic check

```bash
df -h
docker system df
```

If `/` falls below roughly `15%` free:

- clean Docker build cache
- review old images
- review host logs under `/var/log`
