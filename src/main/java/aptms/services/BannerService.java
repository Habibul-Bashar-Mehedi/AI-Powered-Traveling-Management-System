package aptms.services;

import aptms.dto.BannerDTO;

import java.util.List;
import java.util.UUID;

public interface BannerService {

    List<BannerDTO> getAllBanners();

    List<BannerDTO> getActiveBanners();

    BannerDTO createBanner(BannerDTO dto);

    BannerDTO updateBanner(UUID id, BannerDTO dto);

    void deleteBanner(UUID id);

    BannerDTO setActive(UUID id, boolean active);
}
