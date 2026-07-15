package com.homefood.admin.repository;

import com.homefood.admin.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, Long> {
    java.util.List<District> findAllByOrderByNameAsc();

    java.util.Optional<District> findByNameIgnoreCase(String name);
}
