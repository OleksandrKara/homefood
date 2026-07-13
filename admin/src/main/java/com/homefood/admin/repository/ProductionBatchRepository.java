package com.homefood.admin.repository;

import com.homefood.admin.entity.BatchStatus;
import com.homefood.admin.entity.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
    java.util.List<ProductionBatch> findAllByOrderByBatchDateDescCreatedAtDesc();

    java.util.List<ProductionBatch> findByStatusOrderByBatchDateAsc(BatchStatus status);
}
