# SFTP Deploy Package

If you want to deploy through SFTP instead of `git clone` on the server, use the package script below.

## Build the package

Run this in the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\deployment\prepare-sftp-package.ps1 -CreateZip
```

Generated output:

- Folder: `deployment\_dist\nucosmos-pos-sftp-package`
- Zip: `deployment\_dist\nucosmos-pos-sftp-package.zip`

## Recommended upload path

```bash
/srv/nucosmos-pos
```

## Minimum commands on the server

```bash
cd /srv/nucosmos-pos/deployment
cp .env.prod.example .env.prod
nano .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

## Verify after startup

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
curl http://127.0.0.1/actuator/health
```

## Required values

- `POSTGRES_PASSWORD`
- `DB_PASSWORD`
- `JWT_SECRET_BASE64`
- `FRONTEND_ORIGIN`

For a same-domain deployment such as `https://nucosmos.io`:

- `FRONTEND_ORIGIN=https://nucosmos.io`
- `VITE_API_BASE_URL=` leave empty

That lets the frontend call the backend through the same-domain `/api` path.
