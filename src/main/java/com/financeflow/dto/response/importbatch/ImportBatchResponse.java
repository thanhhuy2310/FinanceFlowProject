package com.financeflow.dto.response.importbatch;

import com.financeflow.enums.ImportBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchResponse {

    private Long id;
    private String fileName;
    private LocalDateTime importedAt;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private ImportBatchStatus status;
    private String errorMessage;
    @Builder.Default
    private List<ImportRowFailureResponse> failures = List.of();
}
