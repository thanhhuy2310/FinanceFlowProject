package com.financeflow.mapper;

import com.financeflow.dto.request.rule.RuleRequest;
import com.financeflow.dto.response.rule.RuleResponse;
import com.financeflow.entity.Rule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Rule toEntity(RuleRequest request);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    RuleResponse toResponse(Rule rule);
}
