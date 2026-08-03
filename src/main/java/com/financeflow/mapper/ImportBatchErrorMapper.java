package com.financeflow.mapper;

import com.financeflow.dto.response.importbatch.ImportRowFailureResponse;
import com.financeflow.entity.ImportBatchError;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImportBatchErrorMapper {

    ImportRowFailureResponse toResponse(ImportBatchError error);

    List<ImportRowFailureResponse> toResponses(List<ImportBatchError> errors);
}
