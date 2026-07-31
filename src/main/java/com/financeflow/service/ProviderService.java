package com.financeflow.service;

import com.financeflow.dto.response.provider.ProviderResponse;
import com.financeflow.mapper.ProviderMapper;
import com.financeflow.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderMapper providerMapper;

    @Transactional(readOnly = true)
    public List<ProviderResponse> findAll() {
        return providerRepository.findAllByOrderByNameAsc().stream()
                .map(providerMapper::toResponse)
                .toList();
    }
}
