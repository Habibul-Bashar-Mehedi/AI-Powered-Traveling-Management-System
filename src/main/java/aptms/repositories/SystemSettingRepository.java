package aptms.repositories;

import aptms.entities.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID>, JpaSpecificationExecutor<SystemSetting> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);

    boolean existsBySettingKeyAndSettingIdNot(String settingKey, UUID settingId);
}

