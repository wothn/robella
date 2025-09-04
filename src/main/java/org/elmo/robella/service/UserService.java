package org.elmo.robella.service;

import org.elmo.robella.model.User;
import org.elmo.robella.model.UserDTO;
import org.elmo.robella.model.UserResponse;
import org.elmo.robella.repository.UserRepository;
import cn.dev33.satoken.secure.BCrypt;
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
                
                user.setPassword(BCrypt.hashpw(userDTO.getPassword()));
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
                    existingUser.setPassword(BCrypt.hashpw(userDTO.getPassword()));
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
                return Mono.just(BCrypt.checkpw(password, user.getPassword()));
            })
            .defaultIfEmpty(false);
    }
    
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        response.setEmailVerified(Boolean.valueOf(user.getEmailVerified()));
        response.setPhoneVerified(Boolean.valueOf(user.getPhoneVerified()));
        return response;
    }
}