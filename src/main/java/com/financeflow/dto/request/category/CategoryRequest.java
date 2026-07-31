package com.financeflow.dto.request.category;

import com.financeflow.enums.CategoryType;
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
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Category type is required")
    private CategoryType type;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;
}
