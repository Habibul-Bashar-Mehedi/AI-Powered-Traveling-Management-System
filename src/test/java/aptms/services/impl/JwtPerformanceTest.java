package aptms.services.impl;

import aptms.config.properties.JwtConfigProperties;
import aptms.entities.RefreshToken;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.repositories.RefreshTokenRepository;
import aptms.repositories.TokenBlacklistRepository;
import aptms.services.JwtService;
import aptms.services.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Performance tests for JWT token operations.
 * 
 * Tests performance targets:
 * - Token generation throughput (target: < 50ms P95)
 * - Token validation latency (target: < 10ms per request)
 * - Blacklist lookup latency (target: < 5ms P95)
 * 
 * Requirements: NFR-1
 * Validates: Task 15.1 - Write performance tests for token operations
 */
@ExtendWith(MockitoExtension.class)
class JwtPerformanceTest {
    
    private JwtService jwtService;
    private TokenService tokenService;
    private JwtConfigProperties jwtConfig;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private User testUser;
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final long TOKEN_GENERATION_TARGET_P95_MS = 50;
    private static final long TOKEN_VALIDATION_TARGET_MS = 10;
    private static final long BLACKLIST_LOOKUP_TARGET_P95_MS = 5;
    
    @BeforeEach
    void setUp() {
        // Create test configuration
        jwtConfig = new JwtConfigProperties();
        jwtConfig.setSecret("mySecretKeyForJWTTokenGenerationPleaseChangeInProduction256BitMinimum");
        jwtConfig.setAccessTokenTtl(900000); // 15 minutes
        jwtConfig.setRefreshTokenTtl(604800000); // 7 days
        jwtConfig.setIssuer("com.aptms.auth");
        jwtConfig.setAudience("com.aptms.api");
        jwtConfig.setAlgorithm("HS256");
        
        // Create services
        jwtService = new JwtServiceImpl(jwtConfig);
        
        // Mock Redis operations (lenient to avoid unnecessary stubbing errors)
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);
        org.mockito.Mockito.lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        
        tokenService = new TokenServiceImpl(
            refreshTokenRepository,
            tokenBlacklistRepository,
            redisTemplate
        );
        
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("perftest");
        testUser.setEmail("perftest@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
    }
    
    @Test
    void testTokenGenerationThroughput_MeetsP95Target() {
        // Warmup phase
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            jwtService.generateAccessToken(testUser);
        }
        
        // Test phase - measure token generation latency
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            String token = jwtService.generateAccessToken(testUser);
            long endTime = System.nanoTime();
            
            assertNotNull(token);
            long latencyMs = Duration.ofNanos(endTime - startTime).toMillis();
            latencies.add(latencyMs);
        }
        
        // Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) (TEST_ITERATIONS * 0.50));
        long p95 = latencies.get((int) (TEST_ITERATIONS * 0.95));
        long p99 = latencies.get((int) (TEST_ITERATIONS * 0.99));
        long max = latencies.get(TEST_ITERATIONS - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Log results
        System.out.println("\n=== Token Generation Performance ===");
        System.out.println("Iterations: " + TEST_ITERATIONS);
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50: " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms (target: < " + TOKEN_GENERATION_TARGET_P95_MS + " ms)");
        System.out.println("P99: " + p99 + " ms");
        System.out.println("Max: " + max + " ms");
        
        // Assert P95 meets target
        assertTrue(p95 < TOKEN_GENERATION_TARGET_P95_MS,
            String.format("Token generation P95 latency (%d ms) exceeds target (%d ms)",
                p95, TOKEN_GENERATION_TARGET_P95_MS));
    }
    
    @Test
    void testTokenValidationLatency_MeetsTarget() {
        // Generate test tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            tokens.add(jwtService.generateAccessToken(testUser));
        }
        
        // Warmup phase
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            jwtService.validateToken(tokens.get(i % tokens.size()));
        }
        
        // Test phase - measure token validation latency
        List<Long> latencies = new ArrayList<>();
        
        for (String token : tokens) {
            long startTime = System.nanoTime();
            boolean isValid = jwtService.validateToken(token);
            long endTime = System.nanoTime();
            
            assertTrue(isValid);
            long latencyMs = Duration.ofNanos(endTime - startTime).toMillis();
            latencies.add(latencyMs);
        }
        
        // Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) (TEST_ITERATIONS * 0.50));
        long p95 = latencies.get((int) (TEST_ITERATIONS * 0.95));
        long p99 = latencies.get((int) (TEST_ITERATIONS * 0.99));
        long max = latencies.get(TEST_ITERATIONS - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Log results
        System.out.println("\n=== Token Validation Performance ===");
        System.out.println("Iterations: " + TEST_ITERATIONS);
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50: " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms");
        System.out.println("P99: " + p99 + " ms");
        System.out.println("Max: " + max + " ms (target: < " + TOKEN_VALIDATION_TARGET_MS + " ms)");
        
        // Assert max latency meets target (validation should be consistently fast)
        assertTrue(max < TOKEN_VALIDATION_TARGET_MS,
            String.format("Token validation max latency (%d ms) exceeds target (%d ms)",
                max, TOKEN_VALIDATION_TARGET_MS));
    }
    
    @Test
    void testBlacklistLookupLatency_MeetsP95Target() {
        // Mock Redis to return false (not blacklisted) quickly
        org.mockito.Mockito.lenient().when(tokenBlacklistRepository.findByJti(anyString())).thenReturn(java.util.Optional.empty());
        
        // Generate test JTIs
        List<String> jtis = new ArrayList<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            jtis.add(UUID.randomUUID().toString());
        }
        
        // Warmup phase
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            tokenService.isTokenBlacklisted(jtis.get(i % jtis.size()));
        }
        
        // Test phase - measure blacklist lookup latency
        List<Long> latencies = new ArrayList<>();
        
        for (String jti : jtis) {
            long startTime = System.nanoTime();
            boolean isBlacklisted = tokenService.isTokenBlacklisted(jti);
            long endTime = System.nanoTime();
            
            assertFalse(isBlacklisted);
            long latencyMs = Duration.ofNanos(endTime - startTime).toMillis();
            latencies.add(latencyMs);
        }
        
        // Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) (TEST_ITERATIONS * 0.50));
        long p95 = latencies.get((int) (TEST_ITERATIONS * 0.95));
        long p99 = latencies.get((int) (TEST_ITERATIONS * 0.99));
        long max = latencies.get(TEST_ITERATIONS - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Log results
        System.out.println("\n=== Blacklist Lookup Performance ===");
        System.out.println("Iterations: " + TEST_ITERATIONS);
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50: " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms (target: < " + BLACKLIST_LOOKUP_TARGET_P95_MS + " ms)");
        System.out.println("P99: " + p99 + " ms");
        System.out.println("Max: " + max + " ms");
        
        // Assert P95 meets target
        assertTrue(p95 < BLACKLIST_LOOKUP_TARGET_P95_MS,
            String.format("Blacklist lookup P95 latency (%d ms) exceeds target (%d ms)",
                p95, BLACKLIST_LOOKUP_TARGET_P95_MS));
    }
    
    @Test
    void testRefreshTokenGenerationPerformance() {
        // Warmup phase
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            jwtService.generateRefreshToken(testUser);
        }
        
        // Test phase - measure refresh token generation latency
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            String token = jwtService.generateRefreshToken(testUser);
            long endTime = System.nanoTime();
            
            assertNotNull(token);
            long latencyMs = Duration.ofNanos(endTime - startTime).toMillis();
            latencies.add(latencyMs);
        }
        
        // Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) (TEST_ITERATIONS * 0.50));
        long p95 = latencies.get((int) (TEST_ITERATIONS * 0.95));
        long p99 = latencies.get((int) (TEST_ITERATIONS * 0.99));
        long max = latencies.get(TEST_ITERATIONS - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Log results
        System.out.println("\n=== Refresh Token Generation Performance ===");
        System.out.println("Iterations: " + TEST_ITERATIONS);
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50: " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms");
        System.out.println("P99: " + p99 + " ms");
        System.out.println("Max: " + max + " ms");
        
        // Refresh token generation should be very fast (< 10ms)
        assertTrue(p95 < 10,
            String.format("Refresh token generation P95 latency (%d ms) exceeds 10ms", p95));
    }
    
    @Test
    void testTokenClaimExtractionPerformance() {
        // Generate test token
        String token = jwtService.generateAccessToken(testUser);
        
        // Warmup phase
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            jwtService.extractClaims(token);
        }
        
        // Test phase - measure claim extraction latency
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            var claims = jwtService.extractClaims(token);
            long endTime = System.nanoTime();
            
            assertNotNull(claims);
            long latencyMs = Duration.ofNanos(endTime - startTime).toMillis();
            latencies.add(latencyMs);
        }
        
        // Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) (TEST_ITERATIONS * 0.50));
        long p95 = latencies.get((int) (TEST_ITERATIONS * 0.95));
        long p99 = latencies.get((int) (TEST_ITERATIONS * 0.99));
        long max = latencies.get(TEST_ITERATIONS - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Log results
        System.out.println("\n=== Token Claim Extraction Performance ===");
        System.out.println("Iterations: " + TEST_ITERATIONS);
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50: " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms");
        System.out.println("P99: " + p99 + " ms");
        System.out.println("Max: " + max + " ms");
        
        // Claim extraction should be very fast (< 5ms)
        assertTrue(p95 < 5,
            String.format("Token claim extraction P95 latency (%d ms) exceeds 5ms", p95));
    }
    
    @Test
    void testConcurrentTokenGeneration_NoContentionIssues() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int ITERATIONS_PER_THREAD = 100;
        
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        
        // Create multiple threads generating tokens concurrently
        for (int t = 0; t < THREAD_COUNT; t++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                        String token = jwtService.generateAccessToken(testUser);
                        assertNotNull(token);
                        assertTrue(jwtService.validateToken(token));
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });
            threads.add(thread);
        }
        
        // Start all threads
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        long endTime = System.currentTimeMillis();
        
        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(),
            "Concurrent token generation should not throw exceptions: " + exceptions);
        
        // Log results
        long totalTime = endTime - startTime;
        int totalTokens = THREAD_COUNT * ITERATIONS_PER_THREAD;
        double tokensPerSecond = (totalTokens * 1000.0) / totalTime;
        
        System.out.println("\n=== Concurrent Token Generation ===");
        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("Iterations per thread: " + ITERATIONS_PER_THREAD);
        System.out.println("Total tokens: " + totalTokens);
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Throughput: " + String.format("%.2f", tokensPerSecond) + " tokens/sec");
        
        // Should be able to generate at least 100 tokens per second
        assertTrue(tokensPerSecond > 100,
            String.format("Concurrent token generation throughput (%.2f tokens/sec) is too low", tokensPerSecond));
    }
}
