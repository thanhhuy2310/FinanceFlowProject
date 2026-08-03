package com.financeflow.dto.request.rule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RuleRequest {

    @NotBlank(message = "Keyword is required")
    @Size(max = 100, message = "Keyword must not exceed 100 characters")
    private String keyword;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority;

    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
