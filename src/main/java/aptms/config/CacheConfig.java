package aptms.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache configuration using a simple in-memory ConcurrentMap store.
 *
 * Caches:
 *   - vendors-pending : result of AdminVendorService#getPendingVendors()
 *   - vendors-all     : result of AdminVendorService#getAllVendors()
 *
 * Entries are evicted on every mutating operation (approve / reject / suspend / reinstate)
 * so stale data is never served.
 *
 * No external infrastructure (Redis, Memcached) required.  If Redis-backed caching
 * is needed in production, swap ConcurrentMapCacheManager for RedisCacheManager.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_VENDORS_PENDING = "vendors-pending";
    public static final String CACHE_VENDORS_ALL     = "vendors-all";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_VENDORS_PENDING,
                CACHE_VENDORS_ALL
        );
    }
}

