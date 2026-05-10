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
    private Long id;
    
    @Version
    private Integer version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "TEXT")
    private String userInput;
    
    @Column(columnDefinition = "TEXT")
    private String aiResponse;
    
    @Column(length = 100)
    private String sessionId;
    
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
}
