# JWT Authentication Documentation

Welcome to the JWT Authentication documentation for the AI-Powered Travel Management System (APTMS).

## 📚 Documentation Index

This directory contains comprehensive documentation for the JWT authentication system:

### 1. [Business Requirements Document (BRD)](./BRD_JWT_Authentication.md)
**Purpose**: High-level business requirements and justification for JWT authentication

**Contents**:
- Business objectives and goals
- Stakeholder requirements
- Success criteria and KPIs
- Risk assessment
- Cost-benefit analysis

**Audience**: Business stakeholders, product managers, executives

---

### 2. [API Documentation](./API_DOCUMENTATION.md)
**Purpose**: Complete API reference with interactive Swagger UI guide

**Contents**:
- API endpoint specifications
- Request/response examples
- Error code reference
- Authentication guide
- Swagger UI tutorial
- Testing instructions

**Audience**: Frontend developers, API consumers, QA engineers

**Quick Start**:
```bash
# Start the application
mvn spring-boot:run

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

### 3. [Migration Guide](./MIGRATION_GUIDE.md)
**Purpose**: Step-by-step guide for migrating from session-based to JWT authentication

**Contents**:
- 4-phase migration strategy
- Database migration scripts
- Configuration changes
- Frontend integration guide
- Rollback procedures
- Troubleshooting guide

**Audience**: DevOps engineers, backend developers, system administrators

**Migration Phases**:
1. **Phase 1**: Parallel Operation (1 week)
2. **Phase 2**: Gradual Rollout (2 weeks)
3. **Phase 3**: Full Migration (1 week)
4. **Phase 4**: Legacy Removal (1 week)

---

## 🚀 Quick Start Guide

### For Developers

1. **Read the API Documentation**
   - Start with [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
   - Access Swagger UI at `http://localhost:8080/swagger-ui.html`
   - Test endpoints using the interactive interface

2. **Understand the Architecture**
   - Review the [Design Document](../../.kiro/specs/jwt-authentication/design.md)
   - Study the component diagrams and data models
   - Understand the security mechanisms

3. **Implement Frontend Integration**
   - Follow the frontend examples in [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)
   - Implement token storage and HTTP interceptor
   - Handle token refresh and error scenarios

### For DevOps/System Administrators

1. **Review Configuration Requirements**
   - Check environment variables in [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)
   - Set up Redis for token blacklist
   - Configure database connections

2. **Plan the Migration**
   - Read the complete [Migration Guide](./MIGRATION_GUIDE.md)
   - Test rollback procedures in staging
   - Set up monitoring and alerting

3. **Execute Migration**
   - Follow the 4-phase migration strategy
   - Monitor metrics at each phase
   - Be prepared to rollback if needed

### For Business Stakeholders

1. **Understand the Business Case**
   - Read the [BRD](./BRD_JWT_Authentication.md)
   - Review success criteria and KPIs
   - Understand the timeline and resource requirements

2. **Track Progress**
   - Monitor migration phases
   - Review success metrics
   - Provide feedback and approval

---

## 🔑 Key Features

### Security
- ✅ Stateless JWT authentication (RFC 7519 compliant)
- ✅ BCrypt password hashing
- ✅ Refresh token rotation
- ✅ Token blacklist for revocation
- ✅ Account lockout after 5 failed attempts
- ✅ Refresh token reuse detection

### Performance
- ✅ Token generation < 50ms (P95)
- ✅ Token validation < 10ms per request
- ✅ Redis caching for blacklist
- ✅ Horizontal scalability

### Developer Experience
- ✅ Interactive Swagger UI documentation
- ✅ Comprehensive error codes
- ✅ Request/response examples
- ✅ Easy frontend integration

### Operations
- ✅ Feature flag for gradual rollout
- ✅ Zero-downtime migration
- ✅ Quick rollback capability (< 5 minutes)
- ✅ Comprehensive monitoring

---

## 📊 Architecture Overview

```
┌─────────────────┐
│  Frontend       │
│  (Angular)      │
└────────┬────────┘
         │ Bearer Token
         ▼
┌─────────────────┐
│  JWT Filter     │
│  (Spring)       │
└────────┬────────┘
         │ Validate
         ▼
┌─────────────────┐      ┌─────────────┐
│  JWT Service    │◄────►│   Redis     │
│                 │      │  (Blacklist)│
└────────┬────────┘      └─────────────┘
         │
         ▼
┌─────────────────┐      ┌─────────────┐
│  Auth Service   │◄────►│   MySQL     │
│                 │      │  (Tokens)   │
└─────────────────┘      └─────────────┘
```

### Token Flow

1. **Registration/Login**: User provides credentials → Receive access + refresh tokens
2. **API Access**: Include access token in Authorization header → Access granted
3. **Token Refresh**: Access token expires → Use refresh token → Get new token pair
4. **Logout**: Revoke tokens → Add to blacklist → Session terminated

---

## 🔧 Configuration

### Required Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-256-bit-secret-minimum-32-characters-long
JWT_ACCESS_TOKEN_TTL=900000          # 15 minutes
JWT_REFRESH_TOKEN_TTL=604800000      # 7 days
JWT_ISSUER=com.aptms.auth
JWT_AUDIENCE=com.aptms.api

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/travel_db
DB_USERNAME=root
DB_PASSWORD=your-db-password

# Security Configuration
MAX_FAILED_ATTEMPTS=5
LOCKOUT_DURATION_MINUTES=15
```

### Swagger UI Configuration

Access the interactive API documentation:

**Local**: `http://localhost:8080/swagger-ui.html`  
**API Docs**: `http://localhost:8080/api-docs`

---

## 📈 Success Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Authentication Success Rate | > 99.9% | TBD |
| Token Generation Latency (P95) | < 50ms | TBD |
| Token Validation Latency (P95) | < 10ms | TBD |
| System Uptime | > 99.9% | TBD |
| Security Incidents | 0 | TBD |

---

## 🐛 Troubleshooting

### Common Issues

1. **"TOKEN_INVALID" errors**
   - Check JWT secret consistency across instances
   - Verify server time synchronization
   - Confirm algorithm configuration (HS256)

2. **Refresh token reuse false positives**
   - Check for race conditions in token rotation
   - Verify client-side token storage
   - Review network retry logic

3. **High Redis latency**
   - Increase connection pool size
   - Check network latency to Redis
   - Monitor Redis memory usage

4. **Database migration failures**
   - Verify database permissions
   - Check for data conflicts
   - Review foreign key constraints

**For detailed troubleshooting**, see [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md#troubleshooting)

---

## 📞 Support

### Documentation
- **API Reference**: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- **Migration Guide**: [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### Technical Specifications
- **Requirements**: [requirements.md](../../.kiro/specs/jwt-authentication/requirements.md)
- **Design**: [design.md](../../.kiro/specs/jwt-authentication/design.md)
- **Tasks**: [tasks.md](../../.kiro/specs/jwt-authentication/tasks.md)

### Contact
- **Technical Lead**: tech-lead@aptms.com
- **DevOps Team**: devops@aptms.com
- **Security Team**: security@aptms.com
- **On-Call**: +1-555-0100

---

## 🔐 Security

### Reporting Security Issues

If you discover a security vulnerability, please email security@aptms.com immediately. Do not create a public issue.

### Security Best Practices

1. **Never commit secrets** to version control
2. **Use environment variables** for all sensitive configuration
3. **Rotate JWT secrets** regularly (every 90 days)
4. **Monitor for suspicious activity** (failed logins, token reuse)
5. **Keep dependencies updated** (security patches)

---

## 📝 License

Proprietary - AI-Powered Travel Management System (APTMS)

---

## 🎯 Next Steps

### For New Developers
1. ✅ Read [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
2. ✅ Access Swagger UI and test endpoints
3. ✅ Review [design.md](../../.kiro/specs/jwt-authentication/design.md)
4. ✅ Implement frontend integration

### For DevOps
1. ✅ Review [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)
2. ✅ Set up staging environment
3. ✅ Test rollback procedures
4. ✅ Configure monitoring and alerting

### For Product Managers
1. ✅ Review [BRD_JWT_Authentication.md](./BRD_JWT_Authentication.md)
2. ✅ Approve migration timeline
3. ✅ Define success criteria
4. ✅ Plan user communication

---

**Last Updated**: 2025-01-10  
**Version**: 1.0.0  
**Status**: Production Ready

For the latest updates, check the [CHANGELOG](../../CHANGELOG.md)
