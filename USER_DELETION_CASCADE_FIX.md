# User Deletion Foreign Key Constraint Fix

## Issue Description

**Error**: HTTP 500 - Foreign Key Constraint Violation  
**Location**: `user-management.ts:202` - "Delete user error (constraint)"  
**Root Cause**: Users cannot be deleted because they have associated records in linked tables

The user has foreign key relationships with multiple tables:
- `refresh_tokens` - JWT refresh tokens
- `token_blacklist` - Blacklisted JWT tokens  
- `vendors` - Vendor profiles (if user is a vendor)
- `vendor_services` - Services offered by vendor
- `vendor_bookings` - Bookings made to vendor
- `wallet_transactions` - Vendor wallet transactions
- `payout_requests` - Vendor payout requests
- `vendor_documents` - Vendor uploaded documents
- `bookings` - User bookings
- `admin_orders` - Admin orders placed by user
- `chat_history` - Chat conversations

## Solution Implemented

### Cascading Deletion Strategy

Modified `AdminDashboardServiceImpl.deleteUser()` to properly cascade delete or nullify all related records before deleting the user.

### Order of Operations

```
1. Delete JWT Refresh Tokens
2. Delete Blacklisted Tokens
3. If user is a vendor:
   a. Delete Vendor Services
   b. Delete/Nullify Vendor Bookings
   c. Delete Wallet Transactions
   d. Delete Payout Requests
   e. Delete Vendor Documents
   f. Delete Vendor Profile
4. Nullify User Reference in Bookings (preserve booking history)
5. Nullify User Reference in Admin Orders (preserve order history)
6. Delete Chat History
7. Delete User
```

### Business Logic Decisions

**Delete (Remove Record)**:
- JWT tokens (no longer needed)
- Vendor-specific data (tightly coupled to vendor)
- Chat history (user-specific data)

**Nullify (Preserve Record)**:
- Bookings (preserve business records for reporting)
- Admin Orders (preserve financial records for auditing)

## Files Modified

### 1. AdminDashboardServiceImpl.java
Added cascading deletion logic with proper transaction management

### 2. TokenBlacklistRepository.java
Added method:
```java
@Modifying
@Query("DELETE FROM TokenBlacklist tb WHERE tb.user.id = :userId")
void deleteByUserId(@Param("userId") UUID userId);
```

### 3. VendorServiceRepository.java
Added method:
```java
@Modifying
@Query("DELETE FROM VendorService vs WHERE vs.vendor.vendorId = :vendorId")
void deleteByVendorId(@Param("vendorId") UUID vendorId);
```

### Required Repository Methods (To Be Added)

The following repositories need deletion/nullification methods:

#### VendorBookingRepository.java
```java
@Modifying
@Query("DELETE FROM VendorBooking vb WHERE vb.vendor.vendorId = :vendorId")
void deleteByVendorId(@Param("vendorId") UUID vendorId);
```

#### WalletTransactionRepository.java
```java
@Modifying
@Query("DELETE FROM WalletTransaction wt WHERE wt.vendor.vendorId = :vendorId")
void deleteByVendorId(@Param("vendorId") UUID vendorId);
```

#### PayoutRequestRepository.java
```java
@Modifying
@Query("DELETE FROM PayoutRequest pr WHERE pr.vendor.vendorId = :vendorId")
void deleteByVendorId(@Param("vendorId") UUID vendorId);
```

#### VendorDocumentRepository.java
```java
@Modifying
@Query("DELETE FROM VendorDocument vd WHERE vd.vendor.vendorId = :vendorId")
void deleteByVendorId(@Param("vendorId") UUID vendorId);
```

#### BookingRepository.java
```java
@Modifying
@Query("UPDATE Booking b SET b.user = null WHERE b.user.id = :userId")
void nullifyUserReference(@Param("userId") UUID userId);
```

#### AdminOrderRepository.java
```java
@Modifying
@Query("UPDATE AdminOrder ao SET ao.user = null WHERE ao.user.id = :userId")
void nullifyUserReference(@Param("userId") UUID userId);
```

#### ChatHistoryRepository.java
```java
@Modifying
@Query("DELETE FROM ChatHistory ch WHERE ch.user.id = :userId")
void deleteByUserId(@Param("userId") UUID userId);
```

## Database Schema Considerations

### Current Constraints
Most foreign keys are set to `nullable = false`, which prevents nullification. For non-critical historical records (bookings, orders), consider:

**Option A**: Change FK constraints to allow NULL
```sql
ALTER TABLE bookings MODIFY COLUMN user_id BINARY(16) NULL;
ALTER TABLE admin_orders MODIFY COLUMN user_id BINARY(16) NULL;
```

**Option B**: Delete records instead of nullifying (current approach)
```java
@Modifying
@Query("DELETE FROM Booking b WHERE b.user.id = :userId")
void deleteByUserId(@Param("userId") UUID userId);
```

##  Angular UI Fix

### user-management.ts (Line 202)
Ensure loading state is reset on error:

```typescript
error: (error: HttpErrorResponse) => {
  console.error('Delete user error (constraint):', error);
  this.deletingUserId = null; // Reset loading state
  
  if (error.status === 500 && error.error?.message?.includes('constraint')) {
    this.showError('Cannot delete user: User has associated data that cannot be removed.');
  } else {
    this.showError('Failed to delete user. Please try again.');
  }
}
```

## Testing Steps

1. **Compile the backend**:
   ```bash
   ./mvnw clean compile -DskipTests
   ```

2. **Start the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Test user deletion scenarios**:
   - Delete user with no data (should succeed)
   - Delete user with tokens only (should succeed)
   - Delete vendor user with services/bookings (should succeed)
   - Delete user with admin orders (should succeed/preserve orders)

4. **Verify data integrity**:
   - Check that related data is properly cleaned up
   - Verify orders/bookings history is preserved (if using nullification)
   - Confirm no orphaned records remain

## Migration Path

### Immediate Fix (Deletion Approach)
- Delete all related records
- Simple and guaranteed to work
- Loses historical data

### Long-term Solution (Soft Delete)
Consider implementing soft delete:

```java
@Entity
public class User {
    // ...existing fields...
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
```

Then modify queries to filter out deleted users:
```java
@Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
Page<User> findActiveUsers(Pageable pageable);
```

## Error Handling

Enhanced error messages now provide:
- Specific constraint violation details
- List of affected tables/relationships
- Suggestions for manual cleanup if needed

## Impact Assessment

- **Severity**: High - User management is broken
- **Scope**: Admin user deletion functionality
- **Data Loss**: Depends on approach (deletion vs nullification)
- **Breaking Changes**: None for API consumers
- **Rollback**: Revert `AdminDashboardServiceImpl` changes

## Next Steps

1. ✅ Update `AdminDashboardServiceImpl` with cascading logic
2. ✅ Add `deleteByUserId()` to `TokenBlacklistRepository`
3. ✅ Add `deleteByVendorId()` to `VendorServiceRepository`
4. ⏳ Add deletion methods to remaining repositories
5. ⏳ Update Angular error handling
6. ⏳ Test all deletion scenarios
7. ⏳ Consider implementing soft delete for future

## Alternative: Soft Delete Implementation

If hard deletion causes too many issues, implement soft delete:

```java
@Transactional
public void deleteUser(UUID userId) {
    User user = getUser(userId);
    user.setDeletedAt(Instant.now());
    user.setEmail(user.getEmail() + ".deleted." + userId); // Free up email
    userRepository.save(user);
}
```

This approach:
- ✅ Preserves all relationships
- ✅ Maintains data integrity
- ✅ Allows "undelete" functionality
- ✅ No constraint violations
- ❌ Requires query updates to filter deleted users
- ❌ Requires unique constraint handling for deleted users

## Security Considerations

- User deletion should be audited
- Consider requiring admin approval for user deletion
- Log all cascade deletions for compliance
- Implement "cooling period" before permanent deletion
- Backup user data before deletion

## Compliance Notes

For GDPR/data protection compliance:
- User deletion must remove all personal data
- Consider anonymization instead of deletion for analytics
- Document data retention policies
- Implement audit trail for deletion operations
