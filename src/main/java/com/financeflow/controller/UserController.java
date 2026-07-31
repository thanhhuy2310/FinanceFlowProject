package com.financeflow.controller;

import com.financeflow.dto.request.user.LoginRequest;
import com.financeflow.dto.request.user.RegisterRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.user.LoginResponse;
import com.financeflow.dto.response.user.UserResponse;
import com.financeflow.repository.UserRepository;
import com.financeflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
           @Valid @RequestBody RegisterRequest request
            ){
        UserResponse user =userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user,"Register successfully"));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>>login(
            @Valid @RequestBody LoginRequest request
            ){
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(response,"Login successfully")
        );
    }
}
