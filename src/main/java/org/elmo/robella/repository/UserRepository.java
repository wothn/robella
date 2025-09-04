package org.elmo.robella.repository;

import org.elmo.robella.model.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    
    Mono<User> findByUsername(String username);
    
    Mono<User> findByEmail(String email);
    
    Flux<User> findByActive(Boolean active);
    
    Flux<User> findByRole(String role);
    
    Mono<Boolean> existsByUsername(String username);
    
    Mono<Boolean> existsByEmail(String email);
    
    Flux<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Mono<Void> deleteByUsername(String username);
    
    Mono<Void> deleteByEmail(String email);
}