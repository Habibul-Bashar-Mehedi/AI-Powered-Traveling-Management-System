package aptms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "aptms.repositories")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
    // JPA configuration is handled through application properties
    // This class enables JPA features like auditing and transaction management
}
