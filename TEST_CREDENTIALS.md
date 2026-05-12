# Test Account Credentials

## ✅ System is Ready!

Both the backend and frontend are now running successfully.

## 🔗 Access URLs

- **Frontend**: http://localhost:4200
- **Login Page**: http://localhost:4200/login
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## 👤 Test Accounts

### Vendor Account
- **Email**: `vendor@test.com`
- **Password**: `Vendor@123`
- **Role**: VENDOR
- **Status**: APPROVED (pre-approved with full vendor profile)
- **Business**: Dhaka Express Tours

### Admin Account
- **Email**: `admin@test.com`
- **Password**: `Admin@123`
- **Role**: ADMIN

### Regular User Account
- **Email**: `user@test.com`
- **Password**: `User@1234`
- **Role**: USER

## 🎯 How to Login as Vendor

1. Open your browser and navigate to: http://localhost:4200/login
2. Enter the vendor credentials:
   - Email: `vendor@test.com`
   - Password: `Vendor@123`
3. Click "Sign In"
4. You will be automatically redirected to the vendor dashboard

## 📝 Notes

- The vendor account is **pre-approved** and has a complete vendor profile
- JWT authentication is enabled with 24-hour access tokens
- All passwords meet the minimum 8-character requirement
- The dev database was freshly created to avoid migration issues

## 🔧 Services Running

- ✅ Spring Boot Backend (Port 8080) - with dev profile
- ✅ MySQL Database (travel_db_dev)
- ✅ Angular Frontend (Port 4200)
- ✅ JWT Authentication Enabled

## 🐛 If Login Still Doesn't Work

1. Check browser console (F12) for any errors
2. Verify backend is running: `curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"email":"vendor@test.com","password":"Vendor@123"}'`
3. Verify frontend is running: Browse to http://localhost:4200
4. Clear browser cache and cookies
5. Make sure you're using the exact credentials (case-sensitive)

## 🎉 Success!

The vendor login should now work perfectly. After logging in, you'll be redirected to `/vendor/dashboard` (or wherever the vendor dashboard route is configured).

