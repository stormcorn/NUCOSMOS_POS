# nucosmos.io on Apache Without cPanel Account

Use this guide if:

- your VPS already runs `cPanel/WHM` for other sites
- you do **not** want to add `nucosmos.io` as a paid cPanel domain
- `NUCOSMOS POS` is already running in Docker on `127.0.0.1:8080`
- the admin web should be served from `https://nucosmos.io/erp`

Current verified app status on the VPS:

- admin web: `http://127.0.0.1:8080`
- backend API: `http://127.0.0.1:8080/api/v1/health`

Target public URLs:

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

Suggested target path on the VPS:

```bash
/etc/httpd/conf.d/nucosmos.io.conf
```

Example:

```bash
cp /srv/nucosmos-pos/deployment/apache/nucosmos.io.conf /etc/httpd/conf.d/nucosmos.io.conf
```

## 4. Test and reload Apache

```bash
httpd -t
systemctl reload httpd
```

If reload fails, inspect:

```bash
journalctl -u httpd -n 100 --no-pager
```

## 5. Verify HTTP routing

```bash
curl -H "Host: nucosmos.io" http://127.0.0.1/
curl -H "Host: nucosmos.io" http://127.0.0.1/api/v1/health
```

Expected:

- `/` redirects to `/erp/`
- `/erp/` returns the Vue app HTML
- `/api/v1/health` returns `status=UP`

## 6. HTTPS options

Because `nucosmos.io` is not managed as a cPanel site, AutoSSL will not manage this host for you.

Recommended options:

- put `Cloudflare` in front and use its proxy + SSL
- install a manual `Let's Encrypt` certificate for Apache

For the fastest path, you can:

1. finish HTTP proxy first
2. verify `nucosmos.io` works over port `80`
3. then choose Cloudflare or manual Let's Encrypt

## 7. Rollback

If you need to revert:

```bash
rm -f /etc/httpd/conf.d/nucosmos.io.conf
httpd -t
systemctl reload httpd
```
