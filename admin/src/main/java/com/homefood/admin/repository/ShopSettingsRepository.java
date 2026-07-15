package com.homefood.admin.repository;

import com.homefood.admin.entity.ShopSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopSettingsRepository extends JpaRepository<ShopSettings, Long> {

    /** There is exactly one row (id=1), seeded by V11 - this just makes call sites read cleanly. */
    default ShopSettings getSingleton() {
        return findById(1L).orElseGet(() -> {
            ShopSettings settings = new ShopSettings();
            settings.setId(1L);
            return save(settings);
        });
    }
}
