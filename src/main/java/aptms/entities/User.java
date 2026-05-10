package aptms.entities;

import aptms.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

@Table(name = "users")
@Entity
@Audited
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Integer version;
    
    @Column(nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    @Column(length = 10)
    private String countryId;
}
