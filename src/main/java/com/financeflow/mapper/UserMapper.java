package com.financeflow.mapper;

import com.financeflow.dto.request.user.RegisterRequest;
import com.financeflow.dto.response.user.UserResponse;
import com.financeflow.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "accounts", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "importBatches", ignore = true)
    @Mapping(target = "rules", ignore = true)
    User toEntity(RegisterRequest request);

    UserResponse toResponse(User user);

}
