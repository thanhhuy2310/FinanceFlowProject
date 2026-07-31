package com.financeflow.service;

import com.financeflow.dto.request.user.LoginRequest;
import com.financeflow.dto.response.user.LoginResponse;
import com.financeflow.entity.User;
import com.financeflow.enums.UserRole;
import com.financeflow.exception.InvalidCredentialException;
import com.financeflow.mapper.UserMapper;
import com.financeflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.financeflow.dto.request.user.RegisterRequest;
import com.financeflow.dto.response.user.UserResponse;
import com.financeflow.exception.EmailAlreadyExistsException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
        private final UserRepository userRepository;
        private final UserMapper userMapper;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        public UserResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new EmailAlreadyExistsException("Email already exists");
                }
                User user = userMapper.toEntity(request);
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setRole(UserRole.USER);
                user.setStatus(true);
                LocalDateTime now = LocalDateTime.now();
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                user = userRepository.save(user);
                return userMapper.toResponse(user);
        }

        public LoginResponse login(LoginRequest request) {
                User user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new InvalidCredentialException(
                                "Invalid email or password"));
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new InvalidCredentialException("Invalid email or password");
                }
                String token = jwtService.generateToken(user);
                UserResponse response = userMapper.toResponse(user);
                return  LoginResponse.builder().token(token).user(response).build();

        }
}
