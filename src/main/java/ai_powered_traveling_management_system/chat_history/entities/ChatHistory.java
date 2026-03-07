package ai_powered_traveling_management_system.chat_history.entities;

import ai_powered_traveling_management_system.user.entities.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
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
