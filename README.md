<div align="center">

# 🧠 SMTS — Smart Travel Management System

**AI-Powered Travel Marketplace**  

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21.2-DD0031?logo=angular)](https://angular.dev)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-7-FF4438?logo=redis)](https://redis.io)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📋 Overview

SMTS is a full-stack **AI-powered travel marketplace** that connects travelers with vendors — tour guides, hotels, transport providers, and more. It features intelligent travel recommendations via **Google Gemini AI**, secure **JWT authentication** with OTP email verification, **SSLCommerz** payment processing, role-based dashboards, and comprehensive booking management.

---

## ✨ Features

| Module | Capabilities |
|--------|-------------|
| **🤖 AI Chat Assistant** | Gemini 2.0 Flash–powered travel recommendations and itinerary planning |
| **🔐 Authentication** | JWT access/refresh token rotation, Redis blacklisting, reuse detection, BCrypt hashing |
| **📧 Email Verification** | OTP-based email verification via Gmail SMTP during registration |
| **👥 Role-Based Access** | Distinct dashboards for `USER`, `VENDOR`, and `ADMIN` roles |
| **🏨 Vendor Marketplace** | Hotels, rooms, transport, tour guides, traditional food/items, destinations |
| **📅 Booking Workflow** | End-to-end booking, history, spending tracking, and receipt PDF generation |
| **💳 Payments** | SSLCommerz sandbox integration with IPN, success/fail/cancel webhooks |
| **📁 File Uploads** | MIME-type validation via Apache Tika |
| **🗄️ Audit Logging** | Hibernate Envers on key entities; structured auth audit logs via Logstash |
| **🔒 Account Security** | Lockout after 5 failed attempts, soft-delete support, feature flags for gradual rollout |
| **📍 Explore Nearby** | Geo-utilities for location-based discovery |

---

## 🧱 Tech Stack

### Backend

| Technology | Purpose |
|-----------|---------|
| Java 21 + Spring Boot 4.0.3 | Core framework |
| Spring Security + JWT (jjwt 0.12.5) | Authentication & authorization |
| Spring Data JPA / Hibernate 7.2 | ORM with Envers auditing |
| MySQL 8.4 | Primary database |
| Redis 7 (Jedis) | Token blacklisting & caching |
| Flyway | Schema migrations (11 scripts) |
| SSLCommerz API v4 | Payment gateway |
| Google Gemini 2.0 Flash | AI chat recommendations |
| Thymeleaf + OpenHTMLtoPDF | Receipt PDF generation |
| Springdoc OpenAPI 2.3 | API documentation (Swagger UI) |
| Testcontainers 1.21.3 | Integration test containers |
| Prometheus / Micrometer | Metrics & monitoring |

### Frontend

| Technology | Purpose |
|-----------|---------|
| Angular 21.2 (Standalone + SSR) | Web framework |
| TypeScript 5.9 | Language |
| RxJS 7.8 | Reactive state management |
| Tailwind CSS 4.1 | Styling |
| Vitest 4.0 | Unit testing |
| Express 5.1 | SSR server |

---

## 🚀 Getting Started

### Prerequisites

- **Java** 21+
- **Node.js** 22+
- **Maven** 3.9+ (or use `./mvnw`)
- **MySQL** 8.4
- **Redis** 7+
- **Docker** & **Docker Compose** (optional)

### 1. Clone & Configure

```bash
git clone https://github.com/Habibul-Bashar-Mehedi/AI-Powered-Traveling-Management-System
cd smts

# Copy environment template and fill in your values
cp .env.example .env
```

### 2. Environment Variables

Configure the following in `.env` (see `.env.example` for the full list):

| Variable | Description |
|----------|-------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL connection |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection |
| `JWT_SECRET` | HS256 signing key (min 256 bits) |
| `GEMINI_API_KEY` | Google Gemini API key |
| `SSLCOMMERZ_STORE_ID` / `SSLCOMMERZ_STORE_PASS` | Payment gateway credentials |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Gmail app credentials for OTP emails |

### 3. Database

```sql
CREATE DATABASE travel_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Migrations run automatically via **Flyway** on startup.

### 4. Backend

```bash
# Using Maven wrapper
./mvnw clean install
./mvnw spring-boot:run

# Or using profiles
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. Frontend

```bash
cd frontend
npm install
ng serve    # http://localhost:4200
```

The dev proxy (`frontend/proxy.conf.js`) forwards `/api` and `/uploads` to the backend at `http://localhost:8080`.

---

## 🐳 Docker (Recommended)

```bash
# Build and start all services
docker compose up --build

# Run in detached mode
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Remove the persistent MySQL volume (resets DB)
docker volume rm ai-powered-traveling-management-system_mysql_data
```

This orchestrates four containers:

| Container | Image | Port | Depends On |
|-----------|-------|------|------------|
| `travel-mysql` | `mysql:8.4` | `3306` | — |
| `travel-redis` | `redis:7-alpine` | `6379` | — |
| `travel-backend` | Built from project root `Dockerfile` | `8080` | mysql, redis |
| `travel-frontend` | Built from `./frontend/Dockerfile` | `4200` | backend |

**Notes:**
- MySQL data persists in a named volume (`ai-powered-traveling-management-system_mysql_data`). Use the `docker volume rm` command above to reset it.
- The backend mounts `./uploads` into the container at `/app/uploads` for file persistence.
- The frontend container sets `BACKEND_URL=http://backend:8080` to reach the API internally.
- All services share the `travel-network` bridge network.

---

## 🧪 Testing

### Backend

```bash
# Unit tests (H2 in-memory)
./mvnw test

# Integration tests (Testcontainers — requires Docker)
./mvnw test -Dspring.profiles.active=integration-test
```

Integration tests spin up real MySQL 8.4 and Redis 7 Alpine containers automatically.

### Frontend

```bash
cd frontend
npx vitest
```

---

## 📚 API Documentation

When the backend is running, visit Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

---

## 📁 Project Structure

```
├── src/                          # Backend (Java / Spring Boot)
│   ├── main/java/aptms/
│   │   ├── api/                  # 33 REST controllers
│   │   ├── services/             # Business logic (46 interfaces + implementations)
│   │   ├── entities/             # 33 JPA entities
│   │   ├── repositories/         # 32 Spring Data repositories
│   │   ├── security/             # JWT filter, SecurityConfig
│   │   ├── config/               # Spring, CORS, JPA, Redis, OpenAPI config
│   │   ├── dto/                  # 25+ request/response DTOs
│   │   ├── enums/                # 32 enums
│   │   ├── exceptions/           # Custom exceptions + global handler
│   │   └── aspects/              # AOP security aspects
│   └── main/resources/
│       ├── application*.properties   # Multi-profile config
│       └── db/migration/             # Flyway migrations
├── frontend/                     # Frontend (Angular 21)
│   └── src/app/
│       ├── admin/                # Admin dashboard
│       ├── vendor/               # Vendor dashboard
│       ├── auth/                 # Login, registration, OTP verification
│       └── ...                   # Features: bookings, payment, profile, etc.
├── docker-compose.yml            # Multi-container orchestration
├── Dockerfile                    # Backend container
└── .env.example                  # Environment variable template
```

---

## 🏗️ Architecture Highlights

- **Stateless REST API** with JWT — tokens stored in Redis for blacklisting & reuse detection
- **Role-based routing** — Angular guards enforce access per role; backend validates via Spring Security
- **AI integration** — Google Gemini API delivers travel suggestions within the chat interface
- **Audit trail** — Hibernate Envers tracks entity history; Logstash captures auth security events
- **Feature flags** — JWT toggle allows gradual migration from session-based auth

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
