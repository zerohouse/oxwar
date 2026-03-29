#!/bin/bash
set -e

REPO=$(git remote get-url origin | sed 's|.*github.com/||;s|\.git$||')
echo "Repo: $REPO"

echo "=== Building server ==="
docker build -t ghcr.io/$REPO/server:latest ./oxwar

echo "=== Building client ==="
docker build -t ghcr.io/$REPO/client:latest ./oxwar-client

echo "=== Pushing to ghcr.io ==="
docker push ghcr.io/$REPO/server:latest
docker push ghcr.io/$REPO/client:latest

echo "=== Done ==="
