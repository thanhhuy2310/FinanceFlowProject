package com.financeflow.dto.response.user;

import com.financeflow.enums.UserRole;

import java.time.LocalDateTime;
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
public class UserResponse {
    private Long id;

    private String fullName;

    private String email;

    private UserRole role;

    private LocalDateTime createdAt;
}
