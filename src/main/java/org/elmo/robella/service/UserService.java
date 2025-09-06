package org.elmo.robella.service;

import org.elmo.robella.model.LoginRequest;
import org.elmo.robella.model.LoginResponse;
import org.elmo.robella.model.User;
import org.elmo.robella.model.UserDTO;
import org.elmo.robella.model.UserResponse;
import org.elmo.robella.repository.UserRepository;
import org.elmo.robella.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public Mono<UserResponse> createUser(UserDTO userDTO) {
        return userRepository.existsByUsername(userDTO.getUsername())
            .flatMap(existsByUsername -> {
                if (existsByUsername) {
                    return Mono.error(new IllegalArgumentException("用户名已存在"));
                }
                return userRepository.existsByEmail(userDTO.getEmail());
            })
            .flatMap(existsByEmail -> {
                if (existsByEmail) {
                    return Mono.error(new IllegalArgumentException("邮箱已存在"));
                }
                
                User user = new User();
                BeanUtils.copyProperties(userDTO, user);
                
                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                user.setActive(true);
                user.setRole("USER");
                user.setEmailVerified("false");
                user.setPhoneVerified("false");
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                
                return userRepository.save(user);
            })
            .map(this::convertToResponse)
            .doOnSuccess(user -> log.info("用户创建成功: {}", user.getUsername()))
            .doOnError(error -> log.error("用户创建失败: {}", error.getMessage()));
    }
    
    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")));
    }
    
    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")));
    }
    
    public Mono<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")));
    }
    
    public Flux<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .map(this::convertToResponse);
    }
    
    public Flux<UserResponse> getUsersByRole(String role) {
        return userRepository.findByRole(role)
            .map(this::convertToResponse);
    }
    
    public Flux<UserResponse> getActiveUsers() {
        return userRepository.findByActive(true)
            .map(this::convertToResponse);
    }
    
    public Mono<UserResponse> updateUser(Long id, UserDTO userDTO) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(existingUser -> {
                if (!existingUser.getUsername().equals(userDTO.getUsername())) {
                    return userRepository.existsByUsername(userDTO.getUsername())
                        .flatMap(exists -> exists ? 
                            Mono.error(new IllegalArgumentException("用户名已存在")) : 
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                if (!existingUser.getEmail().equals(userDTO.getEmail())) {
                    return userRepository.existsByEmail(userDTO.getEmail())
                        .flatMap(exists -> exists ? 
                            Mono.error(new IllegalArgumentException("邮箱已存在")) : 
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                BeanUtils.copyProperties(userDTO, existingUser, "id", "password", "createdAt", "emailVerified", "phoneVerified");
                
                if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                }
                
                existingUser.setUpdatedAt(LocalDateTime.now());
                
                return userRepository.save(existingUser);
            })
            .map(this::convertToResponse)
            .doOnSuccess(user -> log.info("用户更新成功: {}", user.getUsername()))
            .doOnError(error -> log.error("用户更新失败: {}", error.getMessage()));
    }
    
    public Mono<Void> deleteUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> userRepository.deleteById(id))
            .doOnSuccess(v -> log.info("用户删除成功: {}", id))
            .doOnError(error -> log.error("用户删除失败: {}", error.getMessage()));
    }
    
    public Mono<UserResponse> activateUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> {
                user.setActive(true);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
    public Mono<UserResponse> deactivateUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> {
                user.setActive(false);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
    public Mono<UserResponse> updateLastLogin(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> {
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
    public Mono<Boolean> validateUser(String username, String password) {
        return userRepository.findByUsername(username)
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.just(false);
                }
                return Mono.just(passwordEncoder.matches(password, user.getPassword()));
            })
            .defaultIfEmpty(false);
    }
    
    public Mono<LoginResponse> login(LoginRequest loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户名或密码错误")))
            .flatMap(user -> {
                // 检查用户是否激活
                if (!user.getActive()) {
                    return Mono.error(new IllegalArgumentException("用户账号已被停用"));
                }
                
                // 验证密码
                if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                    return Mono.error(new IllegalArgumentException("用户名或密码错误"));
                }
                
                // 更新最后登录时间
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                
                return userRepository.save(user);
            })
            .map(user -> {
                UserResponse userResponse = convertToResponse(user);
                
                // 生成JWT token
                String jwtToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());
                
                return LoginResponse.builder()
                    .user(userResponse)
                    .message("登录成功")
                    .loginTime(LocalDateTime.now())
                    .accessToken(jwtToken)
                    .expiresAt(jwtTokenProvider.getExpirationTime(jwtToken))
                    .build();
            })
            .doOnSuccess(response -> log.info("用户登录成功: {}", response.getUser().getUsername()))
            .doOnError(error -> log.error("用户登录失败: {}", error.getMessage()));
    }
    
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        response.setEmailVerified(Boolean.valueOf(user.getEmailVerified()));
        response.setPhoneVerified(Boolean.valueOf(user.getPhoneVerified()));
        return response;
    }
    
    public Mono<User> getUserByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId);
    }
    
    public Mono<UserResponse> createOAuthUser(User user) {
        return userRepository.existsByUsername(user.getUsername())
            .flatMap(existsByUsername -> {
                if (existsByUsername) {
                    return Mono.error(new IllegalArgumentException("用户名已存在"));
                }
                return userRepository.existsByEmail(user.getEmail());
            })
            .flatMap(existsByEmail -> {
                if (existsByEmail) {
                    return Mono.error(new IllegalArgumentException("邮箱已存在"));
                }
                
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user.setActive(true);
                user.setRole("USER");
                user.setEmailVerified("true");
                // For OAuth users, set password to null since they don't have one
                user.setPassword(null);
                
                return userRepository.save(user);
            })
            .map(this::convertToResponse)
            .doOnSuccess(u -> log.info("OAuth用户创建成功: {}", u.getUsername()))
            .doOnError(error -> log.error("OAuth用户创建失败: {}", error.getMessage()));
    }
    
    public Mono<LoginResponse> createOAuthLoginResponse(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user)
            .map(savedUser -> {
                UserResponse userResponse = convertToResponse(savedUser);
                
                return LoginResponse.builder()
                    .user(userResponse)
                    .message("GitHub登录成功")
                    .loginTime(LocalDateTime.now())
                    .build();
            })
            .doOnSuccess(response -> log.info("OAuth用户登录成功: {}", response.getUser().getUsername()))
            .doOnError(error -> log.error("OAuth用户登录失败: {}", error.getMessage()));
    }
}