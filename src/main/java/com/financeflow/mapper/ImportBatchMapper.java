package com.financeflow.mapper;

import com.financeflow.dto.request.importbatch.ImportBatchRequest;
import com.financeflow.dto.response.importbatch.ImportBatchResponse;
import com.financeflow.entity.ImportBatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImportBatchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "importedAt", ignore = true)
    @Mapping(target = "totalRows", ignore = true)
    @Mapping(target = "successRows", ignore = true)
    @Mapping(target = "failedRows", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    ImportBatch toEntity(ImportBatchRequest request);

    ImportBatchResponse toResponse(ImportBatch importBatch);
}
