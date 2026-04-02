# nucosmos.io on Apache Without cPanel Account

Use this guide if:

- your VPS already runs `cPanel/WHM` for other sites
- you do **not** want to add `nucosmos.io` as a paid cPanel domain
- `NUCOSMOS POS` is already running in Docker on `127.0.0.1:8080`
- the admin web should be served from `https://nucosmos.io/erp`
- the landing page should stay on `https://nucosmos.io/`

Current verified app status on the VPS:

- landing page: `https://nucosmos.io/`
- admin web: `http://127.0.0.1:8080/erp/`
- backend API: `http://127.0.0.1:8080/api/v1/health`

Production rule:

- host `Apache/httpd` is the only public web server
- host `nginx.service` must stay `disabled`
- Docker-internal nginx inside `admin-web` is allowed and expected

Target public URLs:

- landing page: `https://nucosmos.io/`
- admin web: `https://nucosmos.io/erp`
- backend API: `https://nucosmos.io/api`

## 1. Point DNS to the VPS

In Namecheap DNS:

- `A @` -> your VPS public IP
- `A www` -> your VPS public IP

Wait until DNS resolves to the VPS.

## 2. Verify Apache proxy modules

On the VPS:

```bash
httpd -M | egrep 'proxy|headers'
```

You should see at least:

- `proxy_module`
- `proxy_http_module`
- `headers_module`

If one is missing, enable it in Apache before continuing.

## 3. Copy the VirtualHost file

Copy this template:

- [deployment/apache/nucosmos.io.conf](/c:/NUCOSMOS_POS/deployment/apache/nucosmos.io.conf)

Suggested target path on the current cPanel/EA4 VPS:

```bash
/etc/apache2/conf.d/includes/post_virtualhost_global.conf
```

Example:

```bash
cp /srv/nucosmos-pos/deployment/apache/nucosmos.io.conf /etc/apache2/conf.d/includes/post_virtualhost_global.conf
```

Important:

- the current VPS does not use `/etc/httpd/conf.d/` for this site
- `nucosmos.io` must be bound explicitly on:
  - `63.250.42.132:80 127.0.0.1:80`
  - `63.250.42.132:443 127.0.0.1:443`
- wildcard `*:443` caused traffic to fall through to other SSL vhosts on this server

## 4. Test and reload Apache

```bash
apachectl -t
/usr/local/cpanel/scripts/restartsrv_httpd
```

If reload fails, inspect:

```bash
journalctl -u httpd -n 100 --no-pager
```

## 5. Verify HTTP routing

```bash
curl -i -H "Host: nucosmos.io" http://127.0.0.1/
curl -i -H "Host: nucosmos.io" http://127.0.0.1/api/v1/health
echo | openssl s_client -connect nucosmos.io:443 -servername nucosmos.io 2>/dev/null | openssl x509 -noout -subject -issuer -ext subjectAltName
```

Expected:

- `/` returns the landing page HTML or the HTTPS redirect
- `/erp/` returns the Vue app HTML
- `/api/v1/health` returns `status=UP`
- the presented certificate contains `DNS:nucosmos.io`

## 6. Current stable reverse proxy shape

The currently verified stable setup is:

- Apache serves `https://nucosmos.io/` directly from `/var/www/nucosmos-cover`
- Apache proxies `/erp/` to `http://127.0.0.1:8080/`
- Apache proxies `/api/` to `http://127.0.0.1:8080/api/`
- container Nginx inside `admin-web` proxies `/api/` onward to `backend:8081`

Even though the backend can also be exposed on `127.0.0.1:8081`, the `8080 -> nginx -> backend`
chain is the version currently validated in production and should be preferred unless you are
actively debugging the backend container.

## 7. VPS reboot recovery

If the VPS has been restarted, first make sure host nginx did not steal `80/443` from Apache:

```bash
systemctl status httpd --no-pager
systemctl status nginx --no-pager
ss -ltnp | grep -E ':80|:443'
```

If `nginx` is listening on `80/443`, restore the correct service ownership:

```bash
systemctl stop nginx
systemctl disable nginx
systemctl enable httpd
systemctl start httpd
apachectl -t
```

Then, if the site still comes back with `503` or `502`, use this sequence:

```bash
systemctl start docker
cd /srv/nucosmos-pos
docker rm -f nucosmos-pos-postgres-prod nucosmos-pos-backend-prod nucosmos-pos-admin-web-prod 2>/dev/null || true
docker network rm deployment_default 2>/dev/null || true
docker network prune -f
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml up -d
systemctl restart httpd
```

Then verify:

```bash
docker compose --env-file deployment/.env.prod -f deployment/docker-compose.prod.yml ps
curl -i http://127.0.0.1:8080/api/v1/health
curl -i https://nucosmos.io/api/v1/health
curl -i https://nucosmos.io/erp/
```

Expected:

- `/api/v1/health` returns `status=UP`
- `/erp/` returns `200`

## 8. HTTPS options

Because `nucosmos.io` is not managed as a cPanel site, AutoSSL will not manage this host for you.

Recommended options:

- put `Cloudflare` in front and use its proxy + SSL
- install a manual `Let's Encrypt` certificate for Apache

For the fastest path, you can:

1. finish HTTP proxy first
2. verify `nucosmos.io` works over port `80`
3. then choose Cloudflare or manual Let's Encrypt

## 9. Rollback

If you need to revert:

```bash
rm -f /etc/apache2/conf.d/includes/post_virtualhost_global.conf
apachectl -t
/usr/local/cpanel/scripts/restartsrv_httpd
```
