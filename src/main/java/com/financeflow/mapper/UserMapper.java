package com.financeflow.mapper;

import com.financeflow.dto.request.user.RegisterRequest;
import com.financeflow.dto.response.user.UserResponse;
import com.financeflow.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(RegisterRequest request);
    UserResponse toResponse(User user);

}
