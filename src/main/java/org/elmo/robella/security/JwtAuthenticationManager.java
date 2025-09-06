package org.elmo.robella.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.service.UserService;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        
        try {
            if (!jwtTokenProvider.validateToken(authToken)) {
                return Mono.empty();
            }
            
            String username = jwtTokenProvider.getUsernameFromToken(authToken);
            
            return userService.getUserByUsername(username)
                    .flatMap(user -> {
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole())
                        );
                        
                        UsernamePasswordAuthenticationToken auth = 
                                new UsernamePasswordAuthenticationToken(username, authToken, authorities);
                        auth.setDetails(user);
                        
                        return Mono.just((Authentication) auth);
                    })
                    .onErrorResume(e -> {
                        log.error("Authentication failed for user: {}", username, e);
                        return Mono.empty();
                    });
            
        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            return Mono.empty();
        }
    }
}