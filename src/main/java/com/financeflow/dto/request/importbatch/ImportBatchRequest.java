package com.financeflow.dto.request.importbatch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchRequest {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;
}
