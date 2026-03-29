$repo = "zerohouse/oxwar"
$keyFile = "$HOME\Documents\keyfiles\musa.pem"
$sshHost = "ec2-user@13.125.102.31"
$remoteDir = "/home/ec2-user"

Write-Host "=== Building server ===" -ForegroundColor Cyan
docker build -t "ghcr.io/$repo/server:latest" ./oxwar

Write-Host "=== Building client ===" -ForegroundColor Cyan
docker build -t "ghcr.io/$repo/client:latest" ./oxwar-client

Write-Host "=== Pushing to ghcr.io ===" -ForegroundColor Cyan
docker push "ghcr.io/$repo/server:latest"
docker push "ghcr.io/$repo/client:latest"

Write-Host "=== Deploying to server ===" -ForegroundColor Cyan
ssh -i $keyFile $sshHost "cd $remoteDir && docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d && docker compose -f docker-compose.prod.yml ps"

Write-Host "=== Done ===" -ForegroundColor Green
