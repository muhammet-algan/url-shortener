#!/usr/bin/env bash
# ============================================
#  URL Shortener — Quick Start Script (Bash)
# ============================================

set -e

echo -e "\033[0;36m🚀 Starting URL Shortener Stack with Docker Compose...\033[0m"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "\033[0;31m❌ Docker is not running! Please start Docker first.\033[0m"
    exit 1
fi

# Build and start services in background
docker compose up -d --build

echo -e "\n\033[0;32m✅ URL Shortener is up and running!\033[0m"
echo -e "\033[0;33m🌐 Application: http://localhost\033[0m"
echo -e "\033[0;33m🩺 Health Check: http://localhost/api/v1/health\033[0m"
echo -e "\nTo stop all services: docker compose down\n"
