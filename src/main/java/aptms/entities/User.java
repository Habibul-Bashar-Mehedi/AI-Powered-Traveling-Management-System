package aptms.entities;

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
    private long id ;
    private String username;
    private String email;
    private String password;
    private String role;
    private String countryId;

}
