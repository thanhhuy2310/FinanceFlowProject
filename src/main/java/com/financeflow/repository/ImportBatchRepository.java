package com.financeflow.repository;
import com.financeflow.entity.ImportBatch;
import com.financeflow.enums.ImportBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch,Long> {
    List<ImportBatch> findByUserId(Long userId);

    Optional<ImportBatch> findByIdAndUserId(Long id, Long userId);

    List<ImportBatch> findByStatus(ImportBatchStatus status);
}
