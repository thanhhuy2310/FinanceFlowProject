package com.financeflow.dto.request.rule;

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
public class RulePreviewRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
