package com.financeflow.service;

import com.financeflow.dto.request.importbatch.ImportBatchRequest;
import com.financeflow.dto.response.importbatch.ImportBatchResponse;
import com.financeflow.entity.ImportBatch;
import com.financeflow.entity.User;
import com.financeflow.enums.ImportBatchStatus;
import com.financeflow.mapper.ImportBatchMapper;
import com.financeflow.repository.ImportBatchRepository;
import com.financeflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportBatchService {

    private final ImportBatchRepository importBatchRepository;
    private final UserRepository userRepository;
    private final ImportBatchMapper importBatchMapper;

    @Transactional
    public ImportBatchResponse create(ImportBatchRequest request) {
        ImportBatch importBatch = importBatchMapper.toEntity(request);
        importBatch.setUser(getCurrentUser());
        importBatch.setFileName(request.getFileName().trim());
        importBatch.setImportedAt(LocalDateTime.now());
        importBatch.setTotalRows(0);
        importBatch.setSuccessRows(0);
        importBatch.setFailedRows(0);
        importBatch.setStatus(ImportBatchStatus.PENDING);

        return importBatchMapper.toResponse(importBatchRepository.save(importBatch));
    }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> findAll() {
        User currentUser = getCurrentUser();
        return importBatchRepository.findByUserId(currentUser.getId()).stream()
                .map(importBatchMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportBatchResponse findById(Long id) {
        return importBatchMapper.toResponse(getOwnedImportBatch(id));
    }

    @Transactional
    public void delete(Long id) {
        importBatchRepository.delete(getOwnedImportBatch(id));
    }

    private ImportBatch getOwnedImportBatch(Long id) {
        User currentUser = getCurrentUser();
        return importBatchRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import batch not found"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
