#!/bin/bash
set -e

echo "=== Pulling latest images ==="
docker compose -f docker-compose.prod.yml pull

echo "=== Starting services ==="
docker compose -f docker-compose.prod.yml up -d

echo "=== Done ==="
docker compose -f docker-compose.prod.yml ps
