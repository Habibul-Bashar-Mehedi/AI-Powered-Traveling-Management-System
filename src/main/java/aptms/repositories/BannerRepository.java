package aptms.repositories;

import aptms.entities.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    List<Banner> findAllByOrderByDisplayOrderAscCreatedAtDesc();

    @Query("SELECT b FROM Banner b WHERE b.active = true " +
            "AND (b.startDate IS NULL OR b.startDate <= :now) " +
            "AND (b.endDate IS NULL OR b.endDate >= :now) " +
            "ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findActiveBanners(@Param("now") Instant now);
}
