param(
    [string]$OutputRoot = "deployment\\_dist",
    [switch]$CreateZip
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputRootPath = Join-Path $repoRoot $OutputRoot
$packageRoot = Join-Path $outputRootPath "nucosmos-pos-sftp-package"

if (Test-Path $packageRoot) {
    Remove-Item -Path $packageRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null

$copyTargets = @(
    "package.json",
    "package-lock.json",
    "tsconfig.json",
    "tsconfig.app.json",
    "tsconfig.node.json",
    "vite.config.ts",
    "postcss.config.js",
    "tailwind.config.ts",
    ".dockerignore",
    ".env.example",
    "Dockerfile.admin-web",
    "README.md",
    "src",
    "public",
    "backend",
    "deployment\docker-compose.prod.yml",
    "deployment\.env.prod.example",
    "deployment\nginx",
    "docs\production-deployment.md"
)

foreach ($target in $copyTargets) {
    $source = Join-Path $repoRoot $target
    if (-not (Test-Path $source)) {
        continue
    }

    $destination = Join-Path $packageRoot $target
    $destinationParent = Split-Path -Path $destination -Parent
    if (-not (Test-Path $destinationParent)) {
        New-Item -ItemType Directory -Path $destinationParent -Force | Out-Null
    }

    Copy-Item -Path $source -Destination $destination -Recurse -Force
}

$cleanupTargets = @(
    "backend\target",
    "backend\backend-run-8081.log"
)

foreach ($target in $cleanupTargets) {
    $path = Join-Path $packageRoot $target
    if (Test-Path $path) {
        Remove-Item -Path $path -Recurse -Force
    }
}

$serverScript = @"
#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="\${1:-\$(pwd)}"
cd "\$PACKAGE_DIR/deployment"

cp .env.prod.example .env.prod 2>/dev/null || true

echo "Edit deployment/.env.prod first, then run:"
echo "docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build"
"@
Set-Content -Path (Join-Path $packageRoot "DEPLOY_AFTER_UPLOAD.sh") -Value $serverScript -Encoding UTF8

$readme = @"
# SFTP Deploy Package

This folder is the upload package for production deployment through SFTP.

## Upload target

Recommended path on the server:

```bash
/srv/nucosmos-pos
```

## Minimum server commands

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

## Required production values

- POSTGRES_PASSWORD
- DB_PASSWORD
- JWT_SECRET_BASE64
- FRONTEND_ORIGIN

If frontend and API share the same domain, leave `VITE_API_BASE_URL` empty.
"@
Set-Content -Path (Join-Path $packageRoot "SFTP-DEPLOY-README.md") -Value $readme -Encoding UTF8

if ($CreateZip) {
    if (-not (Test-Path $outputRootPath)) {
        New-Item -ItemType Directory -Path $outputRootPath -Force | Out-Null
    }

    $zipPath = Join-Path $outputRootPath "nucosmos-pos-sftp-package.zip"
    if (Test-Path $zipPath) {
        Remove-Item -Path $zipPath -Force
    }

    Compress-Archive -Path $packageRoot -DestinationPath $zipPath -Force
    Write-Output "Created ZIP: $zipPath"
}

Write-Output "Package ready: $packageRoot"
