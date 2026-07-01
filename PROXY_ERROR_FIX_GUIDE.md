# Angular Proxy Error Fix Guide

## Problem Summary

**Error:** `connect ECONNREFUSED 127.0.0.1:8080`

**Root Cause:** Spring Boot backend fails to start because Redis is not installed/running. The backend requires Redis for JWT token blacklist caching.

---

## Solution: Install Redis and Fix Components

### ✅ Step 1: Install Redis

```bash
# Update package lists
sudo apt-get update

# Install Redis server
sudo apt-get install redis-server -y

# Start Redis service
sudo systemctl start redis-server

# Enable Redis to start automatically on boot
sudo systemctl enable redis-server

# Verify Redis is running
sudo systemctl status redis-server

# Test Redis connection
redis-cli ping
# Expected output: PONG
```

---

### ✅ Step 2: Verify Database Setup

The `travel_db` MySQL database has already been created. Verify it exists:

```bash
mysql -u root -ppassword -e "SHOW DATABASES LIKE 'travel_db';"
```

---

### ✅ Step 3: Start Spring Boot Backend

```bash
# Navigate to project directory
cd ~/Documents/JAVA/AI-Powered-Traveling-Management-System

# Start Spring Boot backend
./mvnw spring-boot:run
```

**Expected output:**
```
...
INFO  aptms.TMS - Started TMS in X.XXX seconds (JVM running for X.XXX)
INFO  o.s.b.w.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http)
```

**Keep this terminal open** - the backend needs to stay running.

---

### ✅ Step 4: Verify Backend is Responding

Open a **new terminal** and test:

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Expected output:
# {"status":"UP"}
```

---

### ✅ Step 5: Start Angular Frontend

```bash
# Open a new terminal
cd ~/Documents/JAVA/AI-Powered-Traveling-Management-System/frontend

# Install dependencies (if not already done)
npm install

# Start Angular dev server
npm start
# or
ng serve
```

**Expected output:**
```
✔ Browser application bundle generation complete.
Local: http://localhost:4200/
```

---

### ✅ Step 6: Test the Application

1. Open browser: `http://localhost:4200`
2. Try logging in - the proxy should work now
3. Check browser DevTools Network tab - API calls should succeed

---

## ✅ Fixed: FooterComponent Warning

**Issue:** "FooterComponent is not used within the template of Dashboard"

**Fix Applied:** Removed unused `FooterComponent` import from `dashboard.ts`

**File Changed:**
- `/frontend/src/app/dashboard/dashboard.ts` - Removed `FooterComponent` from imports

---

## Verification Checklist

- ✅ Redis installed and running
- ✅ MySQL running with `travel_db` database
- ✅ Spring Boot backend starts without errors
- ✅ Backend health endpoint returns `{"status":"UP"}`
- ✅ Angular frontend starts without compilation warnings
- ✅ Frontend can call backend APIs (no ECONNREFUSED)
- ✅ FooterComponent warning eliminated

---

## Troubleshooting

### If Redis fails to start:
```bash
# Check Redis logs
sudo journalctl -u redis-server -n 50

# Try manual start
redis-server
```

### If backend still crashes:
```bash
# Check for port conflicts
sudo lsof -i :8080

# View backend logs
./mvnw spring-boot:run 2>&1 | tee backend.log
```

### If proxy still fails:
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check Vite proxy config: `frontend/vite.config.ts`
3. Restart both backend and frontend

---

## Configuration Files

### Backend Configuration
- **Database:** MySQL at `localhost:3306/travel_db`
- **Redis:** localhost:6379 (no password)
- **Port:** 8080
- **Config files:**
  - `src/main/resources/application.properties`
  - `.env`

### Frontend Configuration
- **Dev server:** http://localhost:4200
- **Proxy target:** http://localhost:8080
- **Config files:**
  - `frontend/vite.config.ts`
  - `frontend/proxy.conf.json` (if exists)

---

## Summary

The issue was caused by missing Redis installation. Spring Boot requires Redis for JWT token blacklist caching (`TokenServiceImpl` → `RedisTemplate`). Without Redis, the backend fails during bean initialization, preventing it from starting and causing the frontend proxy to get ECONNREFUSED errors.

**Solution:** Install Redis → Start backend → Start frontend → Everything works!
