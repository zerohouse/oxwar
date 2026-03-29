#!/bin/bash
set -e

REPO=${1:?Usage: ./deploy.sh <github-user/repo>}

export GITHUB_REPO=$REPO

echo "=== Pulling latest images ==="
docker compose -f docker-compose.prod.yml pull

echo "=== Starting services ==="
docker compose -f docker-compose.prod.yml up -d

echo "=== Done ==="
docker compose -f docker-compose.prod.yml ps
