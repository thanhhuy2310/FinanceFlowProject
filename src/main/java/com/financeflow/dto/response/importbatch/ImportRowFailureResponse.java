package com.financeflow.dto.response.importbatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportRowFailureResponse {

    private Long id;
    private Integer rowNumber;
    private String errorMessage;
    /** The description found in the CSV row, when available. */
    private String description;
    /** The category name found in the CSV row, when available. */
    private String categoryName;
    private LocalDateTime createdAt;
}
