param(
    [Parameter(Mandatory=$true)]
    [string]$Repo
)

$env:GITHUB_REPO = $Repo

Write-Host "=== Pulling latest images ===" -ForegroundColor Cyan
docker compose -f docker-compose.prod.yml pull

Write-Host "=== Starting services ===" -ForegroundColor Cyan
docker compose -f docker-compose.prod.yml up -d

Write-Host "=== Done ===" -ForegroundColor Green
docker compose -f docker-compose.prod.yml ps
