package aptms.services.impl;

import aptms.dto.BannerDTO;
import aptms.entities.Banner;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.BannerRepository;
import aptms.services.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static aptms.constants.EntityConstants.BANNER;
import static aptms.constants.EntityConstants.ENTITY_NOT_FOUND_MESSAGE;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BannerDTO> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerDTO> getActiveBanners() {
        return bannerRepository.findActiveBanners(Instant.now())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BannerDTO createBanner(BannerDTO dto) {
        Banner entity = new Banner();
        mapDtoToEntity(dto, entity);
        return toDTO(bannerRepository.save(entity));
    }

    @Override
    @Transactional
    public BannerDTO updateBanner(UUID id, BannerDTO dto) {
        Banner entity = getBanner(id);
        mapDtoToEntity(dto, entity);
        return toDTO(bannerRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteBanner(UUID id) {
        Banner entity = getBanner(id);
        bannerRepository.delete(entity);
    }

    @Override
    @Transactional
    public BannerDTO setActive(UUID id, boolean active) {
        Banner entity = getBanner(id);
        entity.setActive(active);
        return toDTO(bannerRepository.save(entity));
    }

    private Banner getBanner(UUID id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException(
                        String.format(ENTITY_NOT_FOUND_MESSAGE, BANNER, id)));
    }

    private void mapDtoToEntity(BannerDTO dto, Banner entity) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setBadgeText(dto.getBadgeText());
        entity.setCtaLabel(dto.getCtaLabel() != null ? dto.getCtaLabel() : "Explore");
        entity.setCtaTarget(dto.getCtaTarget() != null ? dto.getCtaTarget() : "offers");
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
    }

    private BannerDTO toDTO(Banner e) {
        BannerDTO dto = new BannerDTO();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setImageUrl(e.getImageUrl());
        dto.setBadgeText(e.getBadgeText());
        dto.setCtaLabel(e.getCtaLabel());
        dto.setCtaTarget(e.getCtaTarget());
        dto.setActive(e.getActive());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setDisplayOrder(e.getDisplayOrder());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
