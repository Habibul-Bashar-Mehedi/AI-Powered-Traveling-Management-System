package aptms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for token blacklist caching.
 * 
 * Provides:
 * - RedisConnectionFactory with connection pooling
 * - RedisTemplate<String, String> for blacklist operations
 * - Graceful fallback to MySQL if Redis is unavailable
 * 
 * Requirements: NFR-3, NFR-4
 */
@Configuration
public class RedisConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:2000}")
    private long redisTimeout;
    
    @Value("${spring.data.redis.jedis.pool.max-active:8}")
    private int maxActive;
    
    @Value("${spring.data.redis.jedis.pool.max-idle:8}")
    private int maxIdle;
    
    @Value("${spring.data.redis.jedis.pool.min-idle:0}")
    private int minIdle;
    
    /**
     * Create Redis connection factory with connection pooling.
     * 
     * Configures:
     * - Connection timeout for fast failure detection
     * - Connection pooling for performance
     * - Password authentication if configured
     * 
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Configuring Redis connection: {}:{}", redisHost, redisPort);
        
        // Configure Redis standalone connection
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        // Set password if provided
        if (redisPassword != null && !redisPassword.isBlank()) {
            redisConfig.setPassword(redisPassword);
            logger.debug("Redis password authentication enabled");
        }
        
        // Configure Jedis client with connection pooling
        JedisClientConfiguration.JedisClientConfigurationBuilder jedisBuilder = 
            JedisClientConfiguration.builder();
        
        jedisBuilder.connectTimeout(Duration.ofMillis(redisTimeout));
        jedisBuilder.readTimeout(Duration.ofMillis(redisTimeout));
        
        // Connection pool configuration
        jedisBuilder.usePooling();
        
        JedisClientConfiguration jedisConfig = jedisBuilder.build();
        
        JedisConnectionFactory connectionFactory = 
            new JedisConnectionFactory(redisConfig, jedisConfig);
        
        connectionFactory.afterPropertiesSet();
        
        logger.info("Redis connection factory configured successfully");
        
        return connectionFactory;
    }
    
    /**
     * Create RedisTemplate for token blacklist operations.
     * 
     * Configured with:
     * - String key serializer (for JTI keys)
     * - String value serializer (simple "1" value for existence check)
     * - Connection factory with pooling
     * 
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate<String, String>
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("Configuring RedisTemplate for token blacklist");
        
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializers for both keys and values
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        // Enable transaction support
        template.setEnableTransactionSupport(false);
        
        template.afterPropertiesSet();
        
        logger.info("RedisTemplate configured successfully");
        
        return template;
    }
}
