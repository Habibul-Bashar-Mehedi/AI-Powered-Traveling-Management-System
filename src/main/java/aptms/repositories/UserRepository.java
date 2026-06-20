package aptms.repositories;

import aptms.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing user accounts.
 * Updated to use UUID as the primary key for JWT authentication.
 */
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);
    
    /**
     * Find active (non-deleted) user by email.
     * Used for authentication to prevent deleted users from logging in.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);
    
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);

}
