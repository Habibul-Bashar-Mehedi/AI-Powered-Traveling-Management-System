package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Entity
@Audited
@Data
@Table(name = "chatHistories")
public class ChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "user-id",referencedColumnName = "id")
    private User user;
    private String userInput;
    private String aiResponse;
    private String sessionId;
    @CreationTimestamp
    @Column(updatable = false)
    private Date CreatedAt;
}
