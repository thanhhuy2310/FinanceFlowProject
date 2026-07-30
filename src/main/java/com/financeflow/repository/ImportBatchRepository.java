package com.financeflow.repository;
import com.financeflow.entity.ImportBatch;
import com.financeflow.enums.ImportBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchRepository extends JpaRepository<ImportBatch,Long> {
    List<ImportBatch> findByUserId(Long userId);

    List<ImportBatch> findByStatus(ImportBatchStatus status);
}
