# ============================================
#  URL Shortener — Quick Start Script (PowerShell)
# ============================================

Write-Host "`n🚀 Starting URL Shortener Stack with Docker Compose..." -ForegroundColor Cyan

# Check if Docker is running
docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker is not running! Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Start containers
docker compose up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ URL Shortener is up and running!" -ForegroundColor Green
    Write-Host "🌐 Application: http://localhost" -ForegroundColor Yellow
    Write-Host "🩺 Health Check: http://localhost/api/v1/health" -ForegroundColor Yellow
    Write-Host "`nTo stop all services: docker compose down`n" -ForegroundColor Gray
} else {
    Write-Host "`n❌ Failed to start services. Check Docker logs for details." -ForegroundColor Red
}
