#!/bin/bash
set -e

REPO=${1:?Usage: ./build-push.sh <github-user/repo>}

echo "=== Building server ==="
docker build -t ghcr.io/$REPO/server:latest ./oxwar

echo "=== Building client ==="
docker build -t ghcr.io/$REPO/client:latest ./oxwar-client

echo "=== Pushing to ghcr.io ==="
docker push ghcr.io/$REPO/server:latest
docker push ghcr.io/$REPO/client:latest

echo "=== Done ==="
