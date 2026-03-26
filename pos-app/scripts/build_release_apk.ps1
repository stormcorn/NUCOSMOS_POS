param(
    [string]$Label,
    [string]$Revision
)

$ErrorActionPreference = "Stop"

function Get-GitValue {
    param(
        [string[]]$Arguments
    )

    try {
        $value = git @Arguments 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }

        return ($value | Out-String).Trim()
    } catch {
        return $null
    }
}

function Convert-ToSlug {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return "release"
    }

    $slug = $Value.ToLowerInvariant() -replace "[^a-z0-9]+", "-"
    $slug = $slug.Trim("-")

    if ([string]::IsNullOrWhiteSpace($slug)) {
        return "release"
    }

    return $slug
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $scriptDir

$commitMessage = if ($Label) { $Label } else { Get-GitValue -Arguments @("log", "-1", "--pretty=%s") }
$commitSha = if ($Revision) { $Revision } else { Get-GitValue -Arguments @("rev-parse", "--short", "HEAD") }

$apkLabel = Convert-ToSlug $commitMessage
$apkRevision = if ([string]::IsNullOrWhiteSpace($commitSha)) { "local" } else { $commitSha.Trim() }
$apkName = "nucosmos-pos-$apkLabel-$apkRevision.apk"

Push-Location $projectDir
try {
    flutter build apk --release

    $outputDir = Join-Path $projectDir "build\app\outputs\flutter-apk"
    $defaultApk = Join-Path $outputDir "app-release.apk"
    $namedApk = Join-Path $outputDir $apkName

    if (-not (Test-Path $defaultApk)) {
        throw "找不到 release APK：$defaultApk"
    }

    Copy-Item -Path $defaultApk -Destination $namedApk -Force
    Write-Host "已產出：$namedApk"
} finally {
    Pop-Location
}
