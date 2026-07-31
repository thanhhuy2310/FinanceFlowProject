package com.financeflow.dto.response.category;

import com.financeflow.enums.CategoryType;
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
public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;
    private String icon;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
