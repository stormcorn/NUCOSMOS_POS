# Production Deployment

This project is now prepared for production deployment on a Linux VPS.

Recommended target:

- Domain: `nucosmos.io`
- Server: Namecheap Linux VPS
- App root: `/srv/nucosmos-pos`
- If the VPS is managed by `WHM/cPanel`, keep Apache on `80/443` and expose the app stack on `127.0.0.1:8080`

## Production files

- [Dockerfile.admin-web](/c:/NUCOSMOS_POS/Dockerfile.admin-web)
- [backend/Dockerfile](/c:/NUCOSMOS_POS/backend/Dockerfile)
- [deployment/docker-compose.prod.yml](/c:/NUCOSMOS_POS/deployment/docker-compose.prod.yml)
- [deployment/.env.prod.example](/c:/NUCOSMOS_POS/deployment/.env.prod.example)
- [deployment/deploy.sh](/c:/NUCOSMOS_POS/deployment/deploy.sh)
- [deployment/nginx/admin-web.conf](/c:/NUCOSMOS_POS/deployment/nginx/admin-web.conf)
- [GitHub Actions deploy workflow](/c:/NUCOSMOS_POS/.github/workflows/deploy-production.yml)

## Recommended URL structure

- `https://nucosmos.io` -> temporary landing page
- `https://nucosmos.io/erp` -> admin web
- `https://nucosmos.io/api` -> backend API

## Main deployment paths

- VPS deployment guide: [nucosmos-io-vps-deployment.md](/c:/NUCOSMOS_POS/docs/nucosmos-io-vps-deployment.md)
- Apache without cPanel account: [nucosmos-io-apache-without-cpanel.md](/c:/NUCOSMOS_POS/docs/nucosmos-io-apache-without-cpanel.md)
- SFTP upload package: [sftp-deployment-package.md](/c:/NUCOSMOS_POS/docs/sftp-deployment-package.md)

## Local validation already completed

- Frontend build passes
- Backend tests pass
- Production compose config renders successfully

Before real deployment, you still need:

- VPS public IP
- SSH user
- DNS A record for `nucosmos.io`
- GitHub repository secrets for SSH deployment
- Reverse proxy from host Apache or Nginx to `127.0.0.1:8080`

## Current production note

The currently verified production routing on the VPS is:

- Apache serves the landing page from `/var/www/nucosmos-cover`
- Apache proxies `/erp/` to `127.0.0.1:8080`
- Apache proxies `/api/` to `127.0.0.1:8080/api/`
- container Nginx then forwards `/api/` to the backend container

This is the path that was validated after VPS recovery and should be treated as the canonical
reference unless a future deployment intentionally changes it.
