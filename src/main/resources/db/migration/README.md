# Database Migration Scripts for JWT Authentication

This directory contains SQL migration scripts for implementing JWT authentication in the AI-Powered Travel Management System.

## Migration Order

The migrations must be executed in the following order:

1. **V000__migrate_users_id_to_uuid.sql** - Converts User ID from Long to UUID
2. **V001__add_jwt_authentication_fields_to_users.sql** - Adds JWT-related fields to users table
3. **V002__create_refresh_tokens_table.sql** - Creates refresh_tokens table
4. **V003__create_token_blacklist_table.sql** - Creates token_blacklist table

## Important Notes

### Before Running Migrations

1. **Backup your database** - These migrations modify the primary key of the users table
2. **Test in a development environment first**
3. **Review the migration scripts** to ensure they match your database schema
4. **Check for additional foreign keys** - If you have other tables referencing users.id, you'll need to update the V000 migration

### Migration V000 - UUID Conversion

This is the most critical migration as it:
- Converts the users table primary key from BIGINT to BINARY(16) UUID
- Updates all foreign key references in booking and chatHistories tables
- Generates UUIDs for existing users using MySQL's UUID() function

**Warning**: This migration will temporarily drop foreign key constraints and recreate them.

### Running Migrations

#### Option 1: Manual Execution

```bash
# Connect to your MySQL database
mysql -u root -p travel_db

# Run each migration in order
source src/main/resources/db/migration/V000__migrate_users_id_to_uuid.sql
source src/main/resources/db/migration/V001__add_jwt_authentication_fields_to_users.sql
source src/main/resources/db/migration/V002__create_refresh_tokens_table.sql
source src/main/resources/db/migration/V003__create_token_blacklist_table.sql
```

#### Option 2: Using Flyway (Recommended for Production)

If you want to use Flyway for automated migrations:

1. Add Flyway dependency to pom.xml:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

2. Update application.properties:
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

3. Restart the application - Flyway will automatically run the migrations

#### Option 3: JPA Auto-Update (Development Only)

For development, you can let JPA create the schema automatically:

```properties
spring.jpa.hibernate.ddl-auto=update
```

**Note**: This will create the new tables but won't handle the UUID migration properly. You'll still need to run V000 manually.

## Verification

After running the migrations, verify the changes:

```sql
-- Check users table structure
DESCRIBE users;

-- Check refresh_tokens table
DESCRIBE refresh_tokens;

-- Check token_blacklist table
DESCRIBE token_blacklist;

-- Verify foreign keys
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'users';
```

## Rollback

If you need to rollback the migrations:

1. Restore from your database backup
2. Or manually reverse the changes (not recommended)

## Troubleshooting

### Foreign Key Constraint Errors

If you encounter foreign key constraint errors during V000 migration:
1. Check if there are additional tables referencing users.id
2. Update the V000 migration to include those tables
3. Ensure all foreign key constraint names are correct

### UUID Generation Issues

The migration uses MySQL's UUID() function which generates UUID v1 (time-based). If you need a different UUID version:
1. Modify the UPDATE statement in V000
2. Use a custom UUID generation function or application-level UUID generation

### Index Creation Failures

If index creation fails:
1. Check if indexes already exist
2. Verify column names match your schema
3. Ensure you have sufficient privileges

## Support

For issues or questions about these migrations, refer to:
- Design Document: `.kiro/specs/jwt-authentication/design.md`
- Requirements Document: `.kiro/specs/jwt-authentication/requirements.md`
- Tasks Document: `.kiro/specs/jwt-authentication/tasks.md`
