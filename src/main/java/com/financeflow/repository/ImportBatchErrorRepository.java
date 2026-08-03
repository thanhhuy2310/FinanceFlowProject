package com.financeflow.repository;

import com.financeflow.entity.ImportBatchError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchErrorRepository extends JpaRepository<ImportBatchError, Long> {

    List<ImportBatchError> findByImportBatchId(Long importBatchId);
}
