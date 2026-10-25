<#
.SYNOPSIS
Rebuild and restart the Charity docker stack.

.EXAMPLE
.\deploy.ps1                # full: maven package + rebuild backend & frontend images
.\deploy.ps1 -BackendOnly   # only Java / application*.yml changed
.\deploy.ps1 -FrontendOnly  # only Vue code / nginx.conf changed
#>
param(
    [switch]$BackendOnly,   # rebuild backend image only
    [switch]$FrontendOnly,  # rebuild frontend image only
    [switch]$NoCache        # build without cache
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot

try {
    # 1. Repackage the jar. Required whenever Java sources or resources changed,
    #    otherwise the image keeps the old jar.
    if (-not $FrontendOnly) {
        Write-Host "==> [1/2] maven package ..." -ForegroundColor Cyan
        mvn clean package -DskipTests -B
        if ($LASTEXITCODE -ne 0) { throw 'maven package failed, deploy aborted' }
    }

    # 2. Rebuild images. --build is required; `docker compose restart` alone does nothing.
    Write-Host "==> [2/2] docker compose up -d --build ..." -ForegroundColor Cyan
    $buildArgs = @('compose', 'up', '-d', '--build')
    if ($NoCache) { $buildArgs += '--no-cache' }
    if ($BackendOnly)  { $buildArgs += 'backend' }
    if ($FrontendOnly) { $buildArgs += 'frontend' }

    & docker @buildArgs
    if ($LASTEXITCODE -ne 0) { throw 'docker compose build failed' }

    Write-Host "`n==> containers" -ForegroundColor Green
    docker ps --format "table {{.Names}}`t{{.Status}}"

    Write-Host "`n==> backend log (tail 15)" -ForegroundColor Green
    docker logs charity-backend --tail 15
}
finally {
    Pop-Location
}
