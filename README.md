<div align="center">

# ⚡ Kahin URL Shortener

**Production-grade URL shortening service built with Java Spring Boot**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Nginx](https://img.shields.io/badge/Nginx-Load_Balancer-009639?style=for-the-badge&logo=nginx&logoColor=white)](https://nginx.org/)

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [API Reference](#-api-reference) • [Tech Stack](#-tech-stack)

</div>

---

## 🎯 Overview

**Kahin** transforms long URLs into short, shareable links — like TinyURL, but with enterprise-grade features. Built as a portfolio project demonstrating production-ready backend development with Java Spring Boot.

```
Long  → https://example.com/blog/2024/how-to-build-a-production-grade-url-shortener
Short → http://localhost/ab12Cd
```

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔗 **URL Shortening** | Generate unique 6-character Base62 codes or use custom aliases |
| ⚡ **Redis Caching** | Read-through cache for sub-millisecond redirect latency |
| 🚦 **Rate Limiting** | IP-based sliding window (10 req/min) with Redis counters |
| 📊 **Click Analytics** | Track clicks, unique visitors, referers, user agents, timestamps |
| ⏱️ **TTL Support** | Set expiration by hours or specific date; auto-cleanup scheduler |
| 🔀 **Load Balancing** | Nginx reverse proxy distributing traffic across multiple instances |
| 🐳 **Dockerized** | One-command deployment with Docker Compose (5 services) |
| 🛡️ **Production Ready** | Health checks, graceful shutdown, non-root containers, CORS |

---

## 🏗️ Architecture

```
                    ┌─────────────────┐
                    │   Client/User   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Nginx (Port 80)│
                    │  Load Balancer  │
                    │  Rate Limiting  │
                    │  Gzip Compress  │
                    └───┬─────────┬───┘
                        │         │
              ┌─────────▼──┐  ┌──▼─────────┐
              │ Spring Boot│  │ Spring Boot │
              │  App  (1)  │  │  App  (2)   │
              │  Port 8080 │  │  Port 8080  │
              └──┬──────┬──┘  └──┬──────┬───┘
                 │      │        │      │
           ┌─────▼──┐ ┌─▼────────▼─┐ ┌─▼────────┐
           │ Redis  │ │            │ │PostgreSQL│
           │ Cache  │ │   Shared   │ │    DB    │
           │Port6379│ │  Network   │ │Port 5432 │
           └────────┘ └────────────┘ └──────────┘
```

### Request Flow

1. **Client** sends request → **Nginx** load balances to an app instance
2. **POST /api/v1/urls** → Rate limit check (Redis) → Validate URL → Generate short code → Save to PostgreSQL → Cache in Redis
3. **GET /{shortCode}** → Check Redis cache → If miss, query PostgreSQL → Cache result → **302 Redirect** → Async: record click event

---

## 🚀 Quick Start

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Launch (One Command)

```bash
# Clone the repository
git clone https://github.com/muhammet-algan/kahin-url-shortener.git
cd kahin-url-shortener

# Start all services
docker-compose up --build
```

The service will be available at **http://localhost**

### Test It

```bash
# 1. Create a short URL
curl -X POST http://localhost/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://github.com", "ttlHours": 24}'

# 2. Visit the short URL (will redirect)
curl -L http://localhost/ab12Cd

# 3. Check analytics
curl http://localhost/api/v1/urls/ab12Cd/stats
```

---

## 📡 API Reference

### Create Short URL

```http
POST /api/v1/urls
Content-Type: application/json
```

**Request Body:**
```json
{
  "originalUrl": "https://example.com/very-long-url",
  "ttlHours": 168,
  "customCode": "mylink"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `originalUrl` | string | ✅ | The URL to shorten |
| `ttlHours` | number | ❌ | Expiration in hours (default: 168 = 7 days) |
| `expiresAt` | datetime | ❌ | Specific expiration date (overrides ttlHours) |
| `customCode` | string | ❌ | Custom short code (3-12 alphanumeric chars) |

**Response (201 Created):**
```json
{
  "shortCode": "ab12Cd",
  "shortUrl": "http://localhost/ab12Cd",
  "originalUrl": "https://example.com/very-long-url",
  "createdAt": "2026-04-22T10:30:00",
  "expiresAt": "2026-04-29T10:30:00",
  "clickCount": 0
}
```

### Redirect

```http
GET /{shortCode}
→ 302 Found
→ Location: https://example.com/very-long-url
```

### Get Analytics

```http
GET /api/v1/urls/{shortCode}/stats
```

**Response (200 OK):**
```json
{
  "shortCode": "ab12Cd",
  "shortUrl": "http://localhost/ab12Cd",
  "originalUrl": "https://example.com/very-long-url",
  "totalClicks": 142,
  "uniqueVisitors": 89,
  "createdAt": "2026-04-22T10:30:00",
  "lastAccessedAt": "2026-04-22T18:45:00",
  "expiresAt": "2026-04-29T10:30:00",
  "active": true,
  "recentClicks": [
    {
      "clickedAt": "2026-04-22T18:45:00",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0...",
      "referer": "https://twitter.com"
    }
  ],
  "topReferers": {
    "https://twitter.com": 45,
    "https://linkedin.com": 32,
    "direct": 65
  }
}
```

### Delete URL

```http
DELETE /api/v1/urls/{shortCode}
→ 200 OK
```

### Error Responses

| Status | Error | When |
|--------|-------|------|
| `400` | Bad Request | Invalid URL format |
| `404` | Not Found | Short code doesn't exist |
| `409` | Conflict | Custom code already taken |
| `410` | Gone | URL has expired |
| `429` | Too Many Requests | Rate limit exceeded (10/min) |

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language** | Java 17 | LTS, modern features (records, sealed classes) |
| **Framework** | Spring Boot 3.4 | REST API, dependency injection, auto-config |
| **ORM** | Spring Data JPA + Hibernate | Database abstraction, JPQL queries |
| **Database** | PostgreSQL 16 | Primary data store, ACID compliance |
| **Cache** | Redis 7 | URL caching, rate limit counters |
| **Load Balancer** | Nginx | Reverse proxy, round-robin LB, gzip |
| **Containerization** | Docker + Compose | Multi-service orchestration |
| **Build** | Maven | Dependency management, multi-stage builds |

---

## 📁 Project Structure

```
kahin-url-shortener/
├── 🐳 Dockerfile                 # Multi-stage build (Maven → JRE Alpine)
├── 🐳 docker-compose.yml         # 5-service orchestration
├── 📦 pom.xml                    # Maven dependencies
├── 🔀 nginx/
│   └── nginx.conf                # Load balancer config
└── ☕ src/main/java/com/kahin/urlshortener/
    ├── UrlShortenerApplication.java
    ├── config/
    │   ├── AsyncConfig.java      # Thread pool for click tracking
    │   ├── RedisConfig.java      # Redis template setup
    │   └── WebMvcConfig.java     # CORS configuration
    ├── controller/
    │   └── UrlController.java    # REST endpoints
    ├── dto/                      # Request/Response models
    ├── entity/
    │   ├── Url.java              # URL mapping entity
    │   └── ClickEvent.java       # Click analytics entity
    ├── exception/                # Global error handling
    ├── repository/               # JPA repositories
    ├── service/
    │   ├── UrlService.java       # Service interface
    │   ├── RateLimiterService.java
    │   └── impl/
    │       └── UrlServiceImpl.java
    └── util/
        └── Base62Encoder.java    # Short code generator
```

---

## ⚙️ Configuration

All settings are configurable via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | urlshortener | Database name |
| `DB_USERNAME` | postgres | Database user |
| `DB_PASSWORD` | postgres | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `APP_BASE_URL` | http://localhost | Base URL for short links |

---

## 🧪 Key Design Decisions

1. **Base62 over UUID**: 6-char codes (62⁶ = 56.8B combinations) are shorter and URL-friendly
2. **Atomic Click Counter**: `UPDATE ... SET count = count + 1` via JPQL prevents race conditions
3. **Async Click Tracking**: Click events recorded in background threads to minimize redirect latency
4. **Fail-Open Rate Limiter**: If Redis goes down, requests are allowed through (availability > strict limiting)
5. **Read-Through Cache**: Redis cache TTL matches URL TTL for automatic invalidation
6. **Non-Root Container**: Docker runs as `appuser` (UID 1001) following security best practices
7. **Soft Delete**: URLs are deactivated, not deleted — preserving analytics history

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

**Built with ❤️ by [Muhammet Alğan](https://github.com/muhammet-algan)**

⭐ Star this repo if you find it useful!

</div>
