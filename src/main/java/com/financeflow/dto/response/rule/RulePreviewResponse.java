package com.financeflow.dto.response.rule;

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
public class RulePreviewResponse {

    private boolean matched;
    private Long ruleId;
    private String keyword;
    private Long categoryId;
    private String categoryName;
}
