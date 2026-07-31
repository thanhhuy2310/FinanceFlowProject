package com.financeflow.mapper;

import com.financeflow.dto.response.provider.ProviderResponse;
import com.financeflow.entity.Provider;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProviderMapper {

    ProviderResponse toResponse(Provider provider);
}
